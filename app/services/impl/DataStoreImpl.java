package services.impl;

import com.typesafe.config.Config;
import model.ContentType;
import model.Share;
import model.TFile;
import model.user.TgUser;
import model.user.UDbData;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import services.DataStore;
import services.OpdsSearch;
import sql.*;
import states.DirViewer;
import utils.LangMap;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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

        final UUID uuid = generateUuid();
        fs.dropEntry(file.getName(), file.getParentId(), file.getOwner(), tablePrefix + file.getOwner());
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

//    Share getShare(final String id) {
//        return shared.selectShare(id);
//    }

//    boolean entryMissed(final String name, final TgUser user) {
//        return !fs.isEntryExist(name, user.getLocation(), userFsPrefix + user.id);
//    }

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

        UUID next = targetEntryId;

        while (next != null) {
            all.add(new DirViewer(next));
            next = entries.getParent(targetEntryId, tablePrefix + user.id, pathesTree + user.id).getId();
        }

        user.resetState();
        Collections.reverse(all);
        for (int i = 1; i < all.size(); i++)
            user.addState(all.get(i));
    }

    @Override
    public CompletionStage<List<TFile>> doOpdsSearch(final String query, final TgUser user) {
        return opdsSearch.search(query)
                .thenApply(books -> {

                })
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
}
