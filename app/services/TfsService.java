package services;

import model.ContentType;
import model.Share;
import model.TFile;
import model.User;
import model.user.DirGearer;
import model.user.DirViewer;
import model.user.Searcher;
import model.user.Sharer;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import sql.EntryMapper;
import sql.ShareMapper;
import sql.TFileSystem;
import utils.LangMap;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        final Share nShare = new Share();
        nShare.setName(name);
        nShare.setId(tmp);
        nShare.setOwner(user.id);
        nShare.setEntryId(entryId);
        nShare.setSharedTo(sharedTo == null ? 0 : sharedTo.id);

        shared.insertShare(nShare);

        if (sharedTo != null)
            shareAppliedByProducer(nShare, sharedTo, user);
    }

    public void updateMeta(final TFile file, final User user) {
        if (file.isRw())
            fs.updateEntry(file.getName(), file.getParentId(), file.getOptions(), file.getId(), user.id, tablePrefix + file.getOwner());
    }

    public TFile applyShareByLink(final Share share, final User consumer) {
        if (!share.isGlobal())
            return null;

        return applyShare(share, v(LangMap.Value.SHARES_ANONYM, consumer), consumer.lang, consumer.id, consumer.rootId);
    }

    public void shareAppliedByProducer(final Share share, final User target, final User producer) {
        applyShare(share, producer.name, target.lang, target.id, target.rootId);
    }

    @Transactional
    private TFile applyShare(final Share share, final String holderDirName, final String langTag, final long consumerId, final UUID consumerRootId) {
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

        fs.createFsView(userFsPrefix + consumerId, consumerId, tablePrefix + consumerId, fs.selectShareViewsLike(appliedShareViews(consumerId)));

        return shareHolder;
    }

    @Transactional
    public void shareRemoved(final Share share) {
        final List<String> shareViews = fs.selectShareViewsLike(shareSharesViews(share.getId()));

        if (shareViews.isEmpty())
            return;

        // имя шары: fs_share_КомуВыданаШара_КемВыдана_ИдШары
        //           0 _ 1   _ 2            _ 3       _ 4
        final Map<Long, List<String>> byConsumers = shareViews.stream().collect(Collectors.groupingBy(name -> getLong(name.split(Pattern.quote("_"))[2])));

        byConsumers.forEach((key, value) -> {
            value.forEach(name -> fs.dropView(name));
            fs.createFsView(userFsPrefix + key, key, tablePrefix + key, fs.selectShareViewsLike(ownerShareViews(key)));
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

    private String shareSharesViews(final String shareId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + shareId;
    }

    private String ownerShareViews(final long ownerId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + ownerId + "_%";
    }

    private String appliedShareViews(final long ownerId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + ownerId + "_%_%";
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

    public TFile get(final UUID id, final User user) {
        return entries.getEntry(id, userFsPrefix + user.id, pathesTree + user.id);
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

    public List<TFile> gearFolder(final UUID dirId, final DirGearer gearer) {
        return entries.gearDirContent(dirId, gearer.offset, 10, userFsPrefix + gearer.user.id, pathesTree + gearer.user.id);
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
        shared.dropShare(grants.get(0).getId(), owner.user.id);
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
}

