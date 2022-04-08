package services.impl;

import com.typesafe.config.Config;
import model.ContentType;
import model.Share;
import model.TFile;
import model.opds.OpdsBook;
import model.opds.OpdsPage;
import model.opds.TgBook;
import model.user.TgUser;
import model.user.UDbData;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import services.BotApi;
import services.DataStore;
import services.OpdsSearch;
import sql.*;
import states.DirViewer;
import utils.LangMap;
import utils.Strings;
import utils.TFileFactory;
import utils.TextUtils;

import javax.inject.Inject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.04.2022 11:09
 * tfs ☭ sweat and blood
 */
public class DataStoreImpl implements DataStore {
    private static final Logger.ALogger logger = Logger.of(DataStoreImpl.class);
    final static String tablePrefix = "fs_data_", userFsPrefix = "fs_user_", pathesTree = "fs_paths_", sharePrefix = "fs_share_";

    @Inject
    private TfsMapper fs;

    @Inject
    private ShareMapper shared;

    @Inject
    private EntryMapper entries;

    @Inject
    private OpdsMapper bookMapper;

    @Inject
    private OpdsSearch opdsSearch;

    @Inject
    private UserMapper userMapper;

    @Inject
    private Config config;

    @Override
    public String getBotName() {
        return config.getString("service.bot.nick");
    }

    @Transactional
    @Override
    public UUID initUserTables(final long userId) {
        fs.createRootTable(tablePrefix + userId);
        fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());
        fs.createFsTree(pathesTree + userId, userFsPrefix + userId);

        final UUID rootId = generateUuid();
        fs.makeEntry(rootId, "", null, ContentType.DIR, null, 0, tablePrefix + userId);

        return rootId;
    }

    @Override
    public UUID reinitUserTables(final long userId) {
        final UUID rootId;
        if (fs.isTableMissed(tablePrefix + userId)) {
            fs.createRootTable(tablePrefix + userId);
            fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");
            fs.makeEntry((rootId = generateUuid()), "", null, ContentType.DIR, null, 0, tablePrefix + userId);
        } else
            rootId = fs.selectRootId(tablePrefix + userId);

        if (!fs.isViewMissed(userFsPrefix + userId))
            fs.dropView(userFsPrefix + userId);

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());

        if (!fs.isViewMissed(pathesTree + userId))
            fs.dropView(pathesTree + userId);

        fs.createFsTree(pathesTree + userId, userFsPrefix + userId);

        return rootId;
    }

    @Transactional
    private void makeShare(final String name, final TgUser user, final UUID entryId, final TgUser sharedTo) {
        final char[] uuid = TextUtils.generateUuid().toString().replace("-", "").toCharArray();
        String tmp = new String(uuid);

        for (int i = 6; i < uuid.length; i++) {
            tmp = new String(Arrays.copyOfRange(uuid, 0, i));

            if (shared.isIdAvailable(tmp))
                break;
        }

        final Share share = new Share();
        share.setName(name);
        share.setId(tmp);
        share.setOwner(user.id);
        share.setEntryId(entryId);
        share.setSharedTo(sharedTo == null ? 0 : sharedTo.id);

        shared.insertShare(share);

        if (sharedTo != null) {
            share.setFromName(user.fio);
            shareAppliedByProducer(share, sharedTo, user);
        }
    }

    @Override
    public void updateEntry(final TFile file) {
        if (file.isRw())
            fs.updateEntry(file.getName(), file.getParentId(), file.getOptions(), file.getId(), tablePrefix + file.getOwner());
    }

    @Override
    public void updateEntryRef(final TFile file) {
        if (file.isRw())
            fs.updateEntryRef(file.getRefId(), file.getId(), tablePrefix + file.getOwner());
    }

    @Override
    public TFile applyShareByLink(final Share share, final TgUser consumer) {
        if (share.isPersonal())
            return null;

        return applyShare(share, v(LangMap.Value.SHARES_ANONYM, consumer.lng), consumer.lng, consumer.id, consumer.getRoot());
    }

    private void shareAppliedByProducer(final Share share, final TgUser consumer, final TgUser producer) {
        applyShare(share, producer.fio, consumer.lng, consumer.id, consumer.getRoot());
    }

    @Transactional
    private TFile applyShare(final Share share, final String holderDirName, final String langTag, final long consumerId, final UUID consumerRootId) {
        if (share.getOwner() == consumerId)
            return null;

        final List<String> userShares = fs.selectShareViewsLike(sharesByConsumer(consumerId));
        for (final String shareName : userShares)
            if (fs.isEntrySharedTo(shareName, share.getEntryId()))
                return null;


        final String tableName = tablePrefix + consumerId;
        final List<TFile> rootDirs = fs.selectRootDirs(tableName);

        final TFile sharesHomeRoot = rootDirs.stream().filter(TFile::isSharesRoot).findFirst().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setSharesRoot();

            return makeSysDir(v(LangMap.Value.SHARES, langTag), consumerRootId, null, dir.getOptions(), consumerId);
        });

        final List<TFile> subs = fs.selectSubDirs(sharesHomeRoot.getId(), tableName);

        final TFile shareHolder = subs.stream().filter(d -> d.isShareFor() && d.getName().equals(holderDirName)).findAny().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setShareFor(0);

            return makeSysDir(holderDirName, sharesHomeRoot.getId(), dir.getRefId(), dir.getOptions(), consumerId);
        });

        fs.createShareView(
                sharePrefix + consumerId + "_" + share.getOwner() + "_" + share.getId(),
                share.getId(),
                share.getEntryId().toString(),
                shareHolder.getId().toString(),
                tablePrefix + share.getOwner());

        fs.createFsView(userFsPrefix + consumerId, consumerId, tablePrefix + consumerId, fs.selectShareViewsLike(sharesByConsumer(consumerId)));

        return shareHolder;
    }

    @Transactional
    private void shareRemoved(final Share share) {
        final List<String> shareApplies = fs.selectShareViewsLike(sharesByEntry(share.getId()));

        if (shareApplies.isEmpty())
            return;

        // имя шары: fs_share_КомуВыданаШара_КемВыдана_ИдШары
        //           0 _ 1   _ 2            _ 3       _ 4
        final Map<Long, List<String>> byConsumers = shareApplies.stream().collect(Collectors.groupingBy(shareName -> new ShareView(shareName).sharedToId));

        byConsumers.forEach((consumerId, value) -> {
            value.forEach(name -> fs.dropView(name));
            fs.createFsView(userFsPrefix + consumerId, consumerId, tablePrefix + consumerId, fs.selectShareViewsLike(sharesByConsumer(consumerId)));
        });
    }

    @Transactional
    @Override
    public TFile mk(final TFile file) {
        if (file.getParentId() == null) {
            logger.error("Попытка создать что-то в корне: " + file, new Throwable());
            return null;
        }

        final UUID uuid = file.getId() == null ? generateUuid() : file.getId();
        fs.dropEntry(file.getName(), file.getParentId(), file.getOwner(), tablePrefix + file.getOwner());
        fs.makeEntry(uuid, file.getName(), file.getParentId(), file.getType(), file.getRefId(), file.getOptions(), tablePrefix + file.getOwner());

        return entries.getEntry(uuid, userFsPrefix + file.getOwner(), pathesTree + file.getOwner());
    }

    @Transactional
    @Override
    public TFile mkIfMissed(final TFile file) {
        if (file.getParentId() == null) {
            logger.error("Попытка создать что-то в корне: " + file, new Throwable());
            return null;
        }

        if (fs.isEntryExist(file.getName(), file.getParentId(), tablePrefix + file.getOwner()))
            return entries.findEntry(file.getParentId(), file.getName(), tablePrefix + file.getOwner(), pathesTree + file.getOwner());

        final UUID uuid = file.getId() == null ? generateUuid() : file.getId();
        fs.makeEntry(uuid, file.getName(), file.getParentId(), file.getType(), file.getRefId(), file.getOptions(), tablePrefix + file.getOwner());

        return entries.getEntry(uuid, userFsPrefix + file.getOwner(), pathesTree + file.getOwner());
    }

    @Override
    public Share getPublicShare(final String id) {
        return shared.selectPublicShare(id);
    }

    private String sharesByEntry(final String shareId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + shareId;
    }

    private String sharesByProvider(final long sharedByUserId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + sharedByUserId + "_%";
    }

    private String sharesByConsumer(final long sharedToUserId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + sharedToUserId + "_%_%";
    }

    private TFile makeSysDir(final String name0, final UUID parentId, final String refId, final int options, final long userId) {
        final String tableName = tablePrefix + userId;
        String name = name0;
        int counter = 1;

        while (fs.isNameBusy(name, parentId, tableName))
            name = name0 + " (" + (counter++) + ")";

        final TFile dir = new TFile();
        dir.setId(generateUuid());
        dir.setName(name);
        dir.setParentId(parentId);
        dir.setOwner(userId);
        dir.setRefId(refId);
        dir.setOptions(options);

        fs.makeEntry(dir.getId(), dir.getName(), dir.getParentId(), ContentType.DIR, dir.getRefId(), dir.getOptions(), tableName);

        return dir;
    }

    @Override
    public TgBook getStoredBook(final OpdsBook book, final boolean fb2, final boolean epub) {
        return bookMapper.findBook(book.getId(), fb2, epub);
    }

    @Override
    public boolean isEntryMissed(final UUID parentEntryId, final String name, final TgUser user) {
        return !fs.isEntryExist(name, parentEntryId, userFsPrefix + user.id);
    }

    @Override
    public TFile getEntry(final UUID id, final TgUser user) {
        final TFile entry = entries.getEntry(id, userFsPrefix + user.id, pathesTree + user.id);

        if (entry != null)
            entry.setBookStore(entry.isDir() && entry.getId().equals(user.getBookStore()));
        return entry;
    }

    @Override
    public void rm(final UUID entryId, final TgUser user) {
        rm(entryId, user, true);
    }

    @Override
    public void rm(final UUID entryId, final TgUser user, final boolean selfIncluded) {
        final List<TFile> all = entries.getTree(entryId, userFsPrefix + user.id).stream().filter(TFile::isRw).collect(Collectors.toList());

        all.forEach(entry -> {
            shared.getEntryShares(entry.getId(), entry.getOwner()).forEach(this::shareRemoved);
            fs.dropLock(entry.getId());
        });

        all.stream().collect(Collectors.groupingBy(TFile::getOwner))
                .forEach((userId, tFiles) -> {
                    final List<UUID> uuids = tFiles.stream().map(TFile::getId).filter(id -> (selfIncluded || !id.equals(entryId))).collect(Collectors.toList());

                    if (!isEmpty(uuids))
                        entries.rmList(uuids, tablePrefix + userId);
                });

        if (selfIncluded)
            entries.rmSoftLinks(entryId.toString(), tablePrefix + user.id);
    }

    @Override
    public List<TFile> searchFolder(final UUID folderId, final String query, final int offset, final int limit, final long userId) {
        if (isEmpty(query))
            return Collections.emptyList();

        return entries.searchContent("%" + query.toLowerCase().trim() + "%", folderId, offset, 10, userFsPrefix + userId, pathesTree + userId);
    }

    @Override
    public void lockEntry(final TFile entry, final String salt, final String password) {
        fs.dropLock(entry.getId());
        fs.createLock(entry.getId(), salt, password);
        entry.setLocked();
        fs.updateEntry(entry.getName(), entry.getParentId(), entry.getOptions(), entry.getId(), tablePrefix + entry.getOwner());
    }

    @Override
    public void unlockEntry(final TFile entry) {
        fs.dropLock(entry.getId());
        entry.setUnlocked();
        fs.updateEntry(entry.getName(), entry.getParentId(), entry.getOptions(), entry.getId(), tablePrefix + entry.getOwner());
    }

    @Override
    public boolean isPasswordOk(final UUID uuid, final String password) {
        if (isEmpty(password))
            return false;

        final Map<String, Object> accessRow = fs.selectEntryPassword(uuid);

        return accessRow == null || String.valueOf(accessRow.get("password")).equalsIgnoreCase(hash256(accessRow.get("salt") + password));
    }

    @Override
    public int countFolder(final UUID folderId, final long userId) {
        return entries.countDirLs(folderId, userFsPrefix + userId);
    }

    @Override
    public List<TFile> listFolder(final UUID dirId, final int offset, final int limit, final TgUser user) {
        final List<TFile> list = entries.lsDirContent(dirId, offset, limit, userFsPrefix + user.id, pathesTree + user.id);

        if (!isEmpty(user.getBookStore()))
            for (final TFile f : list)
                if (f.getId().equals(user.getBookStore())) {
                    f.setBookStore(true);
                    break;
                }

        return list;
    }

    @Override
    public List<TFile> listFolderLabelsAsFiles(final UUID dirId, final long userId, final int offset, final int limit) {
        return entries.selectFolderLabels(dirId, offset, limit, userFsPrefix + userId, pathesTree + userId);
    }

    @Override
    public int countFolderLabels(final UUID dirId, final long userId) {
        return entries.countDirLs(dirId, userFsPrefix + userId);
    }

    @Override
    public List<String> listFolderLabels(final UUID dirId, final long userId) {
        return entries.lsDirLabels(dirId, userFsPrefix + userId);
    }

    @Override
    public TFile getSingleFolderEntry(final UUID dirId, final int offset, final long userId) {
        final List<TFile> list = entries.lsDirContent(dirId, offset, 1, userFsPrefix + userId, pathesTree + userId);

        return isEmpty(list) ? null : list.get(0);
    }

    @Override
    public TFile getSingleFolderLabel(final UUID dirId, final int offset, final long userId) {
        final List<TFile> list = entries.selectFolderLabels(dirId, offset, 1, userFsPrefix + userId, pathesTree + userId);

        return isEmpty(list) ? null : list.get(0);
    }

    @Override
    public int countSearch(final UUID dirId, final String query, final long userId) {
        return entries.countSearch("%" + query.toLowerCase().trim() + "%", dirId, userFsPrefix + userId);
    }

    // entry shares
    @Override
    public void dropEntryLink(final UUID entryId, final long owner) {
        shared.dropEntryLink(entryId, owner);
    }

    @Override
    public void makeEntryLink(final UUID entryId, final TgUser owner) {
        makeShare(
                entries.getEntry(entryId, userFsPrefix + owner.id, pathesTree + owner.id).getName(),
                owner,
                entryId,
                null);
    }

    @Override
    public void dropEntryGrant(final UUID entryId, final Share share) {
        shareRemoved(share);
        shared.dropShare(share.getId());
    }

    @Override
    public void changeEntryGrantRw(final UUID entryId, final int idx, final int offset, final long userId) {
        shared.changeGrantRw(entryId, offset + idx, userId);
    }

    @Override
    public Share getEntryLink(final UUID entryId) {
        return shared.getEntryLink(entryId);
    }

    @Override
    public int countEntryGrants(final UUID entryId) {
        return shared.countEntryGrants(entryId);
    }

    @Override
    public List<Share> selectEntryGrants(final UUID entryId, final int offset, final int limit, final long owner) {
        return shared.selectEntryGrants(entryId, offset, limit, owner);
    }

    @Override
    public boolean entryNotGrantedTo(final UUID entryId, final long sharedTo, final long owner) {
        return !shared.isShareExists(entryId, sharedTo, owner);
    }

    @Override
    public void entryGrantTo(final UUID entryId, final TgUser shareTo, final TgUser owner) {
        makeShare(shareTo.fio, owner, entryId, shareTo);
    }

    @Override
    public UDbData getUser(final long id) {return userMapper.getUser(id);}

    @Override
    public void insertUser(final UDbData u) {userMapper.insertUser(u);}

    @Override
    public void updateUser(final UDbData u) {userMapper.updateUser(u);}

    @Override
    public void dropUserShares(final long userId) {
        fs.selectShareViewsLike(sharesByConsumer(userId)).forEach(name -> fs.dropView(name));
    }

    @Override
    public void buildHistoryTo(final UUID targetEntryId, final TgUser user) {
        final List<DirViewer> all = new ArrayList<>(0);
        all.add(new DirViewer(targetEntryId));

        UUID next = entries.getParentId(targetEntryId, tablePrefix + user.id);

        while (next != null) {
            all.add(new DirViewer(next));
            next = entries.getParentId(next, tablePrefix + user.id);
        }

        user.resetState();
        Collections.reverse(all);
        for (int i = 1; i < all.size(); i++)
            user.addState(all.get(i));
    }

    @Override
    public OpdsPage doOpdsSearch(final String query, final int page) {
        return opdsSearch.search(query, page);
    }

    @Override
    public TgBook loadBookFile(final OpdsBook book, final boolean fb2, final boolean epub, final BotApi api) {
        final File file = opdsSearch.loadFile(book, fb2, epub);

        if (file == null)
            return null;

        final TgBook b = new TgBook();
        b.title = book.getTitle();
        b.epub = epub;
        b.fb = fb2;
        b.year = book.getYear();
        b.id = book.getId();

        final Fb2Meta meta;

        if (fb2)
            meta = parseFb2Meta(file);
        else if (epub) {
            meta = new Fb2Meta();
            try {
                meta.authors.addAll(book.getAuthors());
                if (!isEmpty(book.getGenres()))
                    meta.genres.addAll(book.getGenres());
                else
                    meta.genres.add("other");
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else
            meta = null;

        if (meta != null && !isEmpty(meta.genres))
            b.genres = String.join(Strings.delim, meta.genres);

        if (meta != null && !isEmpty(meta.authors))
            b.authors = String.join(Strings.delim, meta.authors);

        b.file = file;

        return b;
    }

    @Override
    public Fb2Meta parseFb2Meta(final File file) {
        final Fb2Meta meta = new Fb2Meta();

        try {
            final XMLEventReader reader;
            if (file.getName().toLowerCase().endsWith(".zip")) {
                final ZipFile zip = new ZipFile(file);
                final ZipEntry entry = zip.stream().filter(e -> e.getName().endsWith(".fb2")).findAny().orElse(null);
                if (entry == null) {
                    logger.warn("Didnt find any fb2 inside zip");
                    return null;
                }

                reader = XMLInputFactory.newInstance().createXMLEventReader(zip.getInputStream(entry));
            } else
                reader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(file));

            boolean inTitle = false, inLocalTitle = false, inAuthor = false;
            Author author = null;

            LOOP:
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (nextEvent.isStartElement()) {
                    final StartElement startElement = nextEvent.asStartElement();
                    String content = "";
                    try {content = notNull(reader.nextEvent().asCharacters().getData());} catch (final Exception ignore) {}

                    switch (startElement.getName().getLocalPart()) {
                        case "title-info":
                            inLocalTitle = true;
                        case "src-title-info":
                            inTitle = true;
                            break;
                        case "author":
                            inAuthor = inTitle;
                            author = new Author();
                            break;
                        case "date":
                            if (inTitle)
                                meta.year = getInt(content);
                            break;
                        case "book-title":
                            if (inLocalTitle || (isEmpty(meta.title) && inTitle))
                                meta.title = content;
                            break;
                        case "genre":
                            if (inTitle)
                                meta.genres.add(content);
                            break;
                        case "first-name":
                            if (inAuthor)
                                author.first = content;
                            break;
                        case "last-name":
                            if (inAuthor)
                                author.last = content;
                            break;
                        case "middle-name":
                            if (inAuthor)
                                author.middle = content;
                            break;
                        case "nickname":
                            if (inAuthor)
                                author.nick = content;
                            break;
                        case "body":
                            break LOOP;
                    }
                } else if (nextEvent.isEndElement()) {
                    final EndElement endElement = nextEvent.asEndElement();
                    switch (endElement.getName().getLocalPart()) {
                        case "title-info":
                        case "src-title-info":
                            inTitle = inAuthor = false;
                            break;
                        case "author":
                            inAuthor = false;
                            if (author != null && author.hasSomething())
                                meta.authors.add(author.toString());
                            break;
                    }
                }
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return meta;
    }

    @Override
    public TFile findEntry(final String title, final UUID parentId, final long userId) {
        return entries.findEntry(parentId, title, tablePrefix + userId, pathesTree + userId);
    }

    @Override
    public List<String> resolveOpdsGenrePath(final String path) {
        return opdsSearch.resolveGenrePath(path);
    }

    @Override
    public void insertBook(final TgBook db) {
        bookMapper.insertBook(db);
    }

    @Override
    public UUID getAndParseFb(final byte[] bytes, final TFile file, final BotApi api, final TgUser user) {
        File tmp = null;
        try {
            tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()), file.getName());
            Files.write(tmp.toPath(), bytes, StandardOpenOption.WRITE);

            final Fb2Meta meta = parseFb2Meta(tmp);

            if (meta == null)
                return null;

            final SortedSet<String> authors = new TreeSet<>(meta.authors);
            final Map<String, String> genres = meta.genres.stream().collect(Collectors.toMap(id -> id, this::getOpdsGenreName));

            final Set<TFile> dirs = mkBookDirs(meta.title, genres.keySet(), authors, user);

            final String nameWithAuthors = meta.title +
                    (isEmpty(authors) ? "" : " [" + authors.stream().map(TextUtils::fio2shortName).collect(Collectors.joining(", ")) + "]") +
                    (meta.year > 0 ? " (" + meta.year + ")" : "");

            final String simpleName = meta.title + (meta.year > 0 ? " (" + meta.year + ")" : "");

            final String nameWithGenres = meta.title +
                    (isEmpty(genres) ? "" : " [" + String.join(", ", new TreeSet<>(genres.values())) + "]") +
                    (meta.year > 0 ? " (" + meta.year + ")" : "");

            final TFile abc = dirs.stream().filter(TFile::isAbc).findFirst().orElseThrow(() -> new RuntimeException("No abc dir"));

            final UUID uuid = mk(TFileFactory.file(meta.title, abc.getId(), user.id, file.refId)).getId();

            dirs.stream().filter(d -> !d.isAbc() && d.isBookDir()).forEach(d -> mkIfMissed(TFileFactory.softLink(d.isAbc() ? simpleName : d.isAuthors() ? nameWithGenres :
                    nameWithAuthors, uuid.toString(), d.getId(), user.id)));

            return uuid;
        } catch (final Exception e) {
            logger.error(file.getName() + " :: " + e.getMessage(), e);
        } finally {
            if (tmp != null)
                try {Files.delete(tmp.toPath());} catch (final Exception ignore) {}
        }

        return null;
    }

    @Override
    public Set<TFile> mkBookDirs(final String name, final Set<String> genresIds, final SortedSet<String> authors, final TgUser user) {
        final Set<TFile> dirs = new HashSet<>();

        if (!isEmpty(genresIds)) {
            final TFile genresDir = mkIfMissed(TFileFactory.dir(LangMap.v(LangMap.Value.BOOKS_GENRES, user.lng), user.getBookStore(), user.id));

            for (final String path : genresIds)
                resolveOpdsGenrePath(path).forEach(genreId -> dirs.add(mkIfMissed(TFileFactory.genresDir(getOpdsGenreName(genreId), genresDir.getId(), user.id))));
        }

        if (!isEmpty(authors)) {
            final TFile authorsDir = mkIfMissed(TFileFactory.dir(LangMap.v(LangMap.Value.BOOKS_AUTHORS, user.lng), user.getBookStore(), user.id));

            for (final String author : authors) {
                final char subChar = author.toUpperCase().charAt(0);
                final TFile joinDir = mkIfMissed(TFileFactory.dir(Character.isAlphabetic(subChar) ? String.valueOf(subChar) : "#", authorsDir.getId(), user.id));

                dirs.add(mkIfMissed(TFileFactory.authorsDir(author, joinDir.getId(), user.id)));
            }
        }

        final TFile abcDir = mkIfMissed(TFileFactory.dir(LangMap.v(LangMap.Value.BOOKS_ABC, user.lng), user.getBookStore(), user.id));
        final char subChar = name.toUpperCase().charAt(0);
        dirs.add(mkIfMissed(TFileFactory.abcDir(Character.isAlphabetic(subChar) ? String.valueOf(subChar) : "#", abcDir.getId(), user.id)));

        return dirs;
    }

    @Override
    public UUID findRootId(final long id) {
        return fs.selectRootId(tablePrefix + id);
    }

    @Override
    public String getOpdsGenreName(final String genreId) {
        return opdsSearch.resolveGenreName(genreId);
    }

    static class ShareView {
        final long sharedById, sharedToId;
        final String shareId;

        ShareView(final String viewName) {
            // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
            final String[] parts = viewName.replace(sharePrefix, "").split("_");

            sharedToId = getLong(parts[0]);
            sharedById = getLong(parts[1]);
            shareId = notNull(parts[2]);
        }
    }

    private static class Author {
        String first, last, middle, nick;

        public boolean hasSomething() {
            return !isEmpty(first) || !isEmpty(last) || !isEmpty(nick);
        }

        @Override
        public String toString() {
            return notNull(notNull(middle) + " " + notNull(first) + " " + last, nick);
        }
    }

    public static class Fb2Meta {
        public final Set<String> genres = new HashSet<>(0), authors = new HashSet<>(0);
        public String title;
        public int year;
    }
}
