package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import model.ContentType;
import model.Share;
import model.TFile;
import model.User;
import model.opds.Book;
import model.opds.Folder;
import model.user.DirGearer;
import model.user.DirViewer;
import model.user.Searcher;
import model.user.Sharer;
import org.mybatis.guice.transactional.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import play.Logger;
import play.libs.Json;
import services.impl.OpdsServiceImpl;
import sql.EntryMapper;
import sql.OpdsMapper;
import sql.ShareMapper;
import sql.TFileSystem;
import utils.LangMap;
import utils.TFileFactory;
import utils.TextUtils;
import utils.Xmls;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 31.05.2020
 * tfs ☭ sweat and blood
 */
public class TfsService {
    private static final Logger.ALogger logger = Logger.of(TfsService.class);
    private final static String tablePrefix = "fs_data_", userFsPrefix = "fs_user_", pathesTree = "fs_paths_", sharePrefix = "fs_share_";

    @Inject
    private TFileSystem fs;

    @Inject
    private ShareMapper shared;

    @Inject
    private EntryMapper entries;

    @Inject
    private OpdsMapper bookMapper;

    @Inject
    private TgApi api;

    @Transactional
    public UUID initUserTables(final long userId) {
        fs.createRootTable(tablePrefix + userId);
        fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());
        fs.createFsTree(pathesTree + userId, userFsPrefix + userId);

        final UUID rootId = generateUuid();
        fs.makeEntry(rootId, "", null, ContentType.DIR, null, 0, tablePrefix + userId);

        return rootId;
    }

    public void reinitUserTables(final long userId) {
        if (fs.isTableMissed(tablePrefix + userId)) {
            fs.createRootTable(tablePrefix + userId);
            fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");
            fs.makeEntry(generateUuid(), "", null, ContentType.DIR, null, 0, tablePrefix + userId);
        }

        if (!fs.isViewMissed(userFsPrefix + userId))
            fs.dropView(userFsPrefix + userId);

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());

        if (!fs.isViewMissed(pathesTree + userId))
            fs.dropView(pathesTree + userId);

        fs.createFsTree(pathesTree + userId, userFsPrefix + userId);
    }

    @Transactional
    private void makeShare(final String name, final User user, final UUID entryId, final User sharedTo) {
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
            share.setFromName(user.name);
            shareAppliedByProducer(share, sharedTo, user);
        }
    }

    public void updateMeta(final TFile file, final User user) {
        if (file.isRw())
            fs.updateEntry(file.getName(), file.getParentId(), file.getOptions(), file.getId(), user.id, tablePrefix + file.getOwner());
    }

    public TFile applyShareByLink(final Share share, final User consumer) {
        if (share.isPersonal())
            return null;

        return applyShare(share, v(LangMap.Value.SHARES_ANONYM, consumer), consumer.lang, consumer.id, consumer.rootId);
    }

    public void shareAppliedByProducer(final Share share, final User consumer, final User producer) {
        applyShare(share, producer.name, consumer.lang, consumer.id, consumer.rootId);
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
    public void shareRemoved(final Share share) {
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
    public TFile mk(final TFile file) {
        if (file.getParentId() == null) {
            logger.error("Попытка создать что-то в корне: " + file, new Throwable());
            return null;
        }

        final UUID uuid = generateUuid();
        fs.dropEntry(file.getName(), file.getParentId(), file.getOwner(), tablePrefix + file.getOwner());
        fs.makeEntry(uuid, file.getName(), file.getParentId(), file.getType(), file.getRefId(), file.getOptions(), tablePrefix + file.getOwner());

        return entries.getEntry(uuid, userFsPrefix + file.getOwner(), pathesTree + file.getOwner());
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

    public Share getPublicShare(final String id) {
        return shared.selectPublicShare(id);
    }

    public boolean entryMissed(final String name, final User user) {
        return !fs.isEntryExist(name, user.entryId(), userFsPrefix + user.id);
    }

    public boolean entryMissed(final String name, final UUID parentEntryId, final long userId) {
        return !fs.isEntryExist(name, parentEntryId, userFsPrefix + userId);
    }

    public TFile get(final UUID id, final User user) {
        return entries.getEntry(id, userFsPrefix + user.id, pathesTree + user.id);
    }

    public TFile get(final UUID id, final long userId) {
        return entries.getEntry(id, userFsPrefix + userId, pathesTree + userId);
    }

    public TFile find(final UUID parentId, final String name, final long userId) {
        return entries.findEntry(parentId, name, userFsPrefix + userId, pathesTree + userId);
    }

    public void rm(final UUID entryId, final User user) {
        final List<TFile> all = entries.getTree(entryId, userFsPrefix + user.id).stream().filter(TFile::isRw).collect(Collectors.toList());

        all.forEach(entry -> {
            shared.getEntryShares(entry.getId(), entry.getOwner()).forEach(this::shareRemoved);
            fs.dropLock(entry.getId());
        });

        all.stream().collect(Collectors.groupingBy(TFile::getOwner))
                .forEach((userId, tFiles) -> entries.rmList(tFiles.stream().map(TFile::getId).collect(Collectors.toList()), tablePrefix + userId));
    }

    public List<TFile> search(final Searcher searcher) {
        if (isEmpty(searcher.query))
            return Collections.emptyList();

        return entries.searchContent("%" + searcher.query.toLowerCase().trim() + "%", searcher.entryId, searcher.offset, 10, userFsPrefix + searcher.user.id, pathesTree + searcher.user.id);
    }

    public void lockEntry(final TFile entry, final String salt, final String password) {
        fs.dropLock(entry.getId());
        fs.createLock(entry.getId(), salt, password);
        entry.setLocked();
        fs.updateEntry(entry.getName(), entry.getParentId(), entry.getOptions(), entry.getId(), entry.getOwner(), tablePrefix + entry.getOwner());
    }

    public void unlockEntry(final TFile entry) {
        fs.dropLock(entry.getId());
        entry.setUnlocked();
        fs.updateEntry(entry.getName(), entry.getParentId(), entry.getOptions(), entry.getId(), entry.getOwner(), tablePrefix + entry.getOwner());
    }

    public boolean passwordFailed(final UUID uuid, final String password) {
        final Map<String, Object> accessRow = fs.selectEntryPassword(uuid);

        return !isEmpty(password) && accessRow != null && accessRow.get("password") != null && accessRow.get("salt") != null && !String.valueOf(accessRow.get("password")).equalsIgnoreCase(hash256(accessRow.get(
                "salt") + password));
    }

    public List<TFile> listFolder(final UUID dirId, final int offset, final int limit, final long userId) {
        return entries.lsDirContent(dirId, offset, limit, userFsPrefix + userId, pathesTree + userId);
    }

    public List<TFile> gearFolder(final UUID dirId, final long userId, final int offset, final int limit) {
        return entries.gearDirContent(dirId, offset, limit, userFsPrefix + userId, pathesTree + userId);
    }

    public int countFolder(final UUID dirId, final long userId) {
        return entries.countDirLs(dirId, userFsPrefix + userId);
    }

    public List<String> listLabels(final UUID dirId, final long userId) {
        return entries.lsDirLabels(dirId, userFsPrefix + userId);
    }

    public TFile getFolderEntry(final UUID dirId, final int idx, final DirViewer viewer) {
        final List<TFile> list = entries.lsDirContent(dirId, viewer.offset + idx, 1, userFsPrefix + viewer.user.id, pathesTree + viewer.user.id);

        return isEmpty(list) ? null : list.get(0);
    }

    public TFile getGearEntry(final UUID dirId, final int idx, final DirGearer gearer) {
        final List<TFile> list = entries.gearDirContent(dirId, idx, 1, userFsPrefix + gearer.user.id, pathesTree + gearer.user.id);

        return isEmpty(list) ? null : list.get(0);
    }

    public int countDirLabels(final UUID id, final long userId) {
        return entries.countDirGear(id, userFsPrefix + userId);
    }

    public TFile getParentOf(final UUID entryId, final User user) {
        return entries.getParent(entryId, userFsPrefix + user.id, pathesTree + user.id);
    }

    public int countSearch(final String query, final UUID dirId, final User user) {
        return entries.countSearch("%" + query.toLowerCase().trim() + "%", dirId, userFsPrefix + user.id);
    }

    // entry shares
    public void dropEntryLink(final UUID entryId, final long owner) {
        shared.dropEntryLink(entryId, owner);
    }

    public void makeEntryLink(final UUID entryId, final User owner) {
        makeShare(
                entries.getEntry(entryId, userFsPrefix + owner.id, pathesTree + owner.id).getName(),
                owner,
                entryId,
                null);
    }

    public void dropEntryGrant(final UUID entryId, final int idx, final Sharer owner) {
        final List<Share> grants = shared.selectEntryGrants(entryId, owner.offset + idx, 1, owner.user.id);
        if (grants.isEmpty())
            return;

        shareRemoved(grants.get(0));
        shared.dropShare(grants.get(0).getId());
    }

    public void changeEntryGrantRw(final UUID entryId, final int idx, final Sharer owner) {
        shared.changeGrantRw(entryId, owner.offset + idx, owner.user.id);
    }

    public Share getEntryLink(final UUID entryId) {
        return shared.getEntryLink(entryId);
    }

    public int countEntryGrants(final UUID entryId) {
        return shared.countEntryGrants(entryId);
    }

    public List<Share> selectEntryGrants(final UUID entryId, final int offset, final int limit, final long owner) {
        return shared.selectEntryGrants(entryId, offset, limit, owner);
    }

    public boolean entryNotGrantedTo(final UUID entryId, final long sharedTo, final long owner) {
        return !shared.isShareExists(entryId, sharedTo, owner);
    }

    public void entryGrantTo(final UUID entryId, final User shareTo, final String name, final User owner) {
        makeShare(name, owner, entryId, shareTo);
    }

    public TFile getSearchEntry(final String query, final int elementIdx, final Searcher searcher) {
        final List<TFile> list = entries.searchContent("%" + query.toLowerCase().trim() + "%", searcher.entryId, searcher.offset + elementIdx, 1, userFsPrefix + searcher.user.id,
                pathesTree + searcher.user.id);

        return list.isEmpty() ? null : list.get(0);
    }

    public void dropUserShares(final long userId) {
        fs.selectShareViewsLike(sharesByConsumer(userId))
                .forEach(name -> fs.dropView(name));
    }

    public int countGearShares(final long userId) {
        return (int) Stream
                .concat(
                        shared.getDirectSharesByConsumerId(userId).stream(),
                        shared.selectById(fs.selectShareViewsLike(sharesByConsumer(userId)).stream().map(shareName -> new ShareView(shareName).shareId).collect(Collectors.toList())).stream()
                       )
                .distinct()
                .count();
    }

    public List<TFile> gearShares(final long userId, final String langTag, final int offset, final int limit) {
        final List<Share> all = Stream
                .concat(
                        shared.getDirectSharesByConsumerId(userId).stream(),
                        shared.selectById(fs.selectShareViewsLike(sharesByConsumer(userId)).stream().map(shareName -> new ShareView(shareName).shareId).collect(Collectors.toList())).stream()
                       )
                .distinct()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        if (isEmpty(all))
            return Collections.emptyList();

        return all.stream()
                .map(share -> {
                    final TFile tf = new TFile();
                    tf.setType(ContentType.LABEL);
                    tf.setName(share.isPersonal()
                            ? notNull(share.getFromName(), "user #" + share.getOwner()) + " => " + share.getName()
                            : LangMap.v(LangMap.Value.SHARES_ANONYM, langTag) + " => " + share.getName());

                    return tf;
                })
                .collect(Collectors.toList());
    }

    public Share getGearShareEntry(final long userId, final int elementIdx, final DirGearer gearer) {
        return Stream
                .concat(
                        shared.getDirectSharesByConsumerId(userId).stream(),
                        shared.selectById(fs.selectShareViewsLike(sharesByConsumer(userId)).stream().map(shareName -> new ShareView(shareName).shareId).collect(Collectors.toList())).stream()
                       )
                .distinct()
                .skip(gearer.offset)
                .limit(10)
                .collect(Collectors.toList()).get(elementIdx);
    }

    public Share getShare(final String shareId) {
        final List<Share> list = shared.selectById(Collections.singletonList(shareId));
        return isEmpty(list) ? null : list.get(0);
    }

    public void unshareFromMe(final String shareId, final User user) {
        final Share share = getShare(shareId);

        if (share == null)
            return;

        if (share.isPersonal())
            shared.dropShare(shareId);

        fs.dropView(sharePrefix + user.id + "_" + share.getOwner() + "_" + share.getId());
        fs.createFsView(userFsPrefix + user.id, user.id, tablePrefix + user.id, fs.selectShareViewsLike(sharesByConsumer(user.id)));
        fs.createFsTree(pathesTree + user.id, userFsPrefix + user.id);
    }

    public void syncOpdsDir(final TFile dir, final User owner) {
        try {
            logger.debug("Loading: " + dir.getRefId());
            final Document doc = getXml(dir.refId);
            final URL base = new URL(dir.getRefId());

            final Function<String, String> urler = path -> {
                try {
                    return new URL(base.getProtocol(), base.getHost(), base.getPort(), path).toExternalForm();
                } catch (MalformedURLException e) {
                    logger.error(e.getMessage(), e);
                }

                return null;
            };

            processChilds(
                    dir.getId(),
                    Xmls.getFolders(doc.getElementsByTagName("entry")),
                    Xmls.getBooks(doc.getElementsByTagName("entry")),
                    urler, dir.getOwner());

            final NodeList links = doc.getElementsByTagName("link");
            for (int i = 0; i < links.getLength(); i++)
                if (links.item(i).hasAttributes()) {
                    boolean next = false;
                    String href = "";

                    for (int j = 0; j < links.item(i).getAttributes().getLength(); j++) {
                        final Node n = links.item(i).getAttributes().item(j);

                        if (n.getNodeName().equals("rel") && n.getNodeValue().equals("next"))
                            next = true;
                        else if (n.getNodeName().equals("href"))
                            href = n.getNodeValue();
                    }

                    if (next && !isEmpty(href)) {
                        dir.setRefId(urler.apply(href));
                        syncOpdsDir(dir, owner);
                        break;
                    }
                }
        } catch (final Exception e) {
            logger.error("Failed to make OPDS from " + dir.getRefId() + " :: " + e.getMessage(), e);
        } finally {
            dir.setOpdsSynced();
            updateMeta(dir, owner);
        }
    }

    private void processChilds(final UUID parentFolderId, final Collection<Folder> subfolders, final Collection<Book> books,
                               final Function<String, String> urler, final long owner) {
        logger.debug("Processing " + subfolders.size() + " folders and " + books.size() + " books");
        subfolders.forEach(f -> mk(TFileFactory.opdsDir(f.getTitle(), urler.apply(f.getPath()), parentFolderId, owner)));

        for (Book book : books) {
            if (bookMapper.bookMissed(urler.apply("/"), book.getTag())) {
                if (!isEmpty(book.getFbLink()))
                    loadFile(urler.apply(book.getFbLink()), book::setFbLink, ".fb2.zip");
                if (!isEmpty(book.getEpubLink()))
                    loadFile(urler.apply(book.getEpubLink()), book::setEpubLink, ".epub");

                bookMapper.insertBook(book);
            } else
                book = bookMapper.findBook(urler.apply("/"), book.getTag());

            try {
                final TFile bookDir = mk(TFileFactory.dir(book.getTitle(), parentFolderId, owner));

                if (!isEmpty(book.getEpubLink()) && entryMissed(book.getTitle() + " [EPUB]", bookDir.getId(), owner))
                    makeTFile(book.getTitle() + " [EPUB]", book.getEpubLink(), bookDir.getId(), owner);

                if (!isEmpty(book.getFbLink()) && entryMissed(book.getTitle() + " [FB2]", bookDir.getId(), owner))
                    makeTFile(book.getTitle() + " [FB2]", book.getFbLink(), bookDir.getId(), owner);
            } catch (final Exception e) {
                logger.error(book + " :: " + e.getMessage(), e);
            }
        }
    }

    private void makeTFile(final String title, final String refId, final UUID parentDirId, final long owner) {
        final TFile file = new TFile();
        file.setOwner(owner);
        file.setName(title);
        file.setParentId(parentDirId);
        file.setType(ContentType.DOCUMENT);
        file.setRefId(refId);

        mk(file);
    }

    private void loadFile(final String url, final Consumer<String> refConsumer, final String ext) {
        try {
            final File tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()), ext);
            try (final FileOutputStream bos = new FileOutputStream(tmp)) {
                getFile(url, bos);
            }

            final JsonNode rpl = api.upload(tmp).toCompletableFuture().join();

            if (rpl.has("document"))
                refConsumer.accept(rpl.get("document").get("file_id").asText());
            else
                throw new IOException("Failed to upload book: " + Json.stringify(rpl));
        } catch (IOException e) {
            logger.error(url + " :: " + e.getMessage(), e);
        }
    }

    private Document getXml(final String url) {
        final AtomicReference<Document> doc = new AtomicReference<>(null);

        get(url, is -> {
            try {
                doc.set(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new InvalidXmlCharacterFilter(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))))));
            } catch (final Exception e) {
                logger.error(url + " :: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });

        return doc.get();
    }

    private void getFile(final String url, final FileOutputStream os) {
        get(url, is -> {
            try {
                int read;
                while ((read = is.read()) != -1)
                    os.write(read);

                os.flush();
            } catch (final Exception any) {
                logger.error(url + " :: " + any.getMessage(), any);
                throw new RuntimeException(any);
            }
        });
    }

    private void get(final String url, final Consumer<InputStream> os) {
        try {
            final URLConnection cn = new URL(url).openConnection();

            cn.setDoInput(true);
            cn.setDoOutput(true);

            ((HttpURLConnection) cn).setRequestMethod("POST");
            ((HttpURLConnection) cn).setInstanceFollowRedirects(false);
            cn.setUseCaches(false);

            cn.setConnectTimeout(15000);
            cn.setReadTimeout(15000);

            cn.connect();

            final int code = ((HttpURLConnection) cn).getResponseCode();

            if (code / 200 == 1 && code % 200 < 100) {
                os.accept(cn.getInputStream());
                return;
            }

            try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(128); final InputStream is = ((HttpURLConnection) cn).getErrorStream()) {
                int read;
                while ((read = is.read()) != -1)
                    bos.write(read);

                throw new Exception("CH response error message: " + new String(bos.toByteArray(), StandardCharsets.UTF_8));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class InvalidXmlCharacterFilter extends FilterReader {
        protected InvalidXmlCharacterFilter(Reader in) {
            super(in);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = super.read(cbuf, off, len);
            if (read == -1) return read;

            for (int i = off; i < off + read; i++) {
                if (!XMLChar.isValid(cbuf[i])) cbuf[i] = '?';
            }
            return read;
        }
    }

    public static class ShareView {
        public final long sharedById, sharedToId;
        public final String shareId;

        public ShareView(final String viewName) {
            // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
            final String[] parts = viewName.replace(sharePrefix, "").split("_");

            sharedToId = getLong(parts[0]);
            sharedById = getLong(parts[1]);
            shareId = notNull(parts[2]);
        }
    }
}

