package services;

import model.Share;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import sql.ShareMapper;
import sql.TFileSystem;
import utils.LangMap;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final static String tablePrefix = "fs_struct_", userFsPrefix = "fs_view_", treePrefix = "fs_tree_", sharePrefix = "fs_share_";

    @Inject
    private TFileSystem fs;

    @Inject
    private ShareMapper shared;

    public void validateFs(final long userId) {
        if (fs.isTableMissed(tablePrefix + userId)) {
            fs.createRootTable(tablePrefix + userId);
            fs.makeEntry(generateUuid(), "", null, ContentType.DIR, null, 0, tablePrefix + userId);
        } else {
            final List<Map<String, Object>> rows = fs.getRawTable(tablePrefix + userId);

            if (rows.isEmpty() || !rows.get(0).containsKey("options")) {
                fs.addUuid(tablePrefix + userId);
                fs.addUuidParent(tablePrefix + userId);
                final Map<Long, UUID> id2uuid = new HashMap<>(0);
                final Map<Long, Object> id2parent = new HashMap<>(0);

                rows.forEach(map -> id2uuid.put((Long) map.get("id"), TextUtils.generateUuid()));

                for (final Long oldId : id2uuid.keySet())
                    try {
                        fs.update2uuids(oldId, id2uuid.get(oldId), tablePrefix + userId);
                    } catch (final Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                fs.old0(userId);
                fs.old1(userId);
                fs.old2(userId);
                fs.old5(userId);
                fs.old4(userId);
                fs.old6(userId);
                fs.old65(userId);
                fs.old7(userId);
                fs.old8(userId);
                fs.old10(userId);
                fs.old11(userId);
                fs.old12(userId);
                fs.old13(userId);
                fs.old14(userId);
                fs.old15(userId);
            }
        }

        if (fs.isIndexMissed(tablePrefix + userId + "_names"))
            fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");

        if (fs.isViewMissed(userFsPrefix + userId))
            fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());

        if (fs.isViewMissed(treePrefix + userId))
            fs.createFsTree(treePrefix + userId, userFsPrefix + userId);
    }

    @Transactional
    public void initUser(final long userId) {
        fs.createRootTable(tablePrefix + userId);
        fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());
        fs.createFsTree(treePrefix + userId, userFsPrefix + userId);

        fs.makeEntry(generateUuid(), "", null, ContentType.DIR, null, 0, tablePrefix + userId);
    }

    public boolean shareExist(final UUID entryId, final long contactId, final User user) {
        return shared.selectShareExist(entryId, contactId, user.getId());
    }

    @Transactional
    public void makeShare(final String name, final User user, final UUID entryId, final long sharedTo, final String langTag) {
        final char[] uuid = TextUtils.generateUuid().toString().replace("-", "").toCharArray();
        String tmp = new String(uuid);

        for (int i = 6; i < uuid.length; i++) {
            tmp = new String(Arrays.copyOfRange(uuid, 0, i));

            if (shared.isShareIdAvailable(tmp))
                break;
        }

        final Share nShare = new Share();
        nShare.setName(name);
        nShare.setId(tmp);
        nShare.setOwner(user.getId());
        nShare.setEntryId(entryId);
        nShare.setSharedTo(sharedTo);

        shared.insertShare(nShare);

        if (nShare.getSharedTo() > 0)
            shareAppliedByProducer(nShare, sharedTo, langTag, user);
    }

    public void updateMeta(final TFile file, final User user) {
        if (file.isRw())
            fs.updateEntry(file.getName(), file.getParentId(), file.getOptions(), file.getId(), user.getId(), tablePrefix + file.getOwner());
    }

    @Transactional
    public void applyShareByLink(final Share share, final User consumer) {
        if (!share.isGlobal())
            return;

        applyShare(share, v(LangMap.Value.SHARES_ANONYM, consumer), consumer.getLang(), consumer.getId());
    }

    @Transactional
    public void shareAppliedByProducer(final Share share, final long consumerId, final String langTag, final User producer) {
        if (share.isGlobal())
            return;

        applyShare(share, producer.getName(), langTag, consumerId);
    }

    private void applyShare(final Share share, final String dirName, final String langTag, final long consumerId) {
        final String tableName = tablePrefix + consumerId;
        final List<TFile> rootDirs = fs.selectRootDirs(tableName);

        final TFile sharesDir = rootDirs.stream().filter(TFile::isSharesRoot).findFirst().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setSharesRoot();

            return makeSysDir(v(LangMap.Value.SHARES, langTag), fs.findRoot(userFsPrefix + consumerId, treePrefix + consumerId).getId(), null, dir.getOptions(), consumerId);
        });

        final List<TFile> subs = fs.selectSubDirs(sharesDir.getId(), tableName);

        final TFile shareParentDir = subs.stream().filter(d -> d.isShareFor() && getLong(d.getRefId()) == share.getOwner()).findAny().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setShareFor(share.getOwner());

            return makeSysDir(dirName, sharesDir.getId(), dir.getRefId(), dir.getOptions(), consumerId);
        });

        fs.createShareView(
                sharePrefix + consumerId + "_" + share.getOwner() + "_" + share.getId(),
                share.getId(),
                share.getEntryId(),
                shareParentDir.getId(),
                tablePrefix + share.getOwner());

        fs.createFsView(userFsPrefix + consumerId, consumerId, tablePrefix + consumerId, fs.selectShareViewsLike(ownerShareViews(consumerId)));
    }

    @Transactional
    public void shareRemoved(final Share share) {
        final List<String> shareViews = fs.selectShareViewsLike(shareSharesViews(share.getId()));

        if (shareViews.isEmpty())
            return;

        final Map<Long, List<String>> byOwners = shareViews.stream().collect(Collectors.groupingBy(name -> getLong(name.split(Pattern.quote("_"))[1])));

        byOwners.forEach((key, value) -> {
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

        final String fsName = userFsPrefix + file.getOwner(), treeName = treePrefix + file.getOwner();

        fs.dropEntry(file.getName(), file.getParentId(), file.getOwner(), fsName);
        fs.makeEntry(generateUuid(), file.getName(), file.getParentId(), file.getType(), file.getRefId(), file.getOptions(), tablePrefix + file.getOwner());
        return fs.findEntry(file.getName(), file.getParentId(), file.getOwner(), fsName, treeName);
    }

    private String shareSharesViews(final String shareId) {
        return sharePrefix + "%_" + shareId;
    }

    private String ownerShareViews(final long ownerId) {
        return sharePrefix + ownerId + "_%";
    }

    private TFile makeSysDir(final String name0, final UUID parentId, final String refId, final int options, final long userId) {
        final String tableName = tablePrefix + userId;
        String name = name0;
        int counter = 1;

        final UUID rootId = fs.findRoot(userFsPrefix + userId, treePrefix + userId).getId();

        while (fs.isNameBusy(name, rootId, tableName))
            name = name0 + " (" + (counter++) + ")";

        fs.makeEntry(generateUuid(), name, parentId, ContentType.DIR, refId, options, tableName);

        return fs.findEntry(name, parentId, userId, userFsPrefix + userId, treePrefix + userId);
    }

    public TFile getRoot(final User user) {
        return fs.findRoot(userFsPrefix + user.getId(), treePrefix + user.getId());
    }

    public Share getPublicShare(final String id) {
        return shared.selectPublicShare(id);
    }

    public List<TFile> list(final User user) {
        final String fsName = userFsPrefix + user.getId(), treeName = treePrefix + user.getId();

        return fs.listChilds(user.current.getId(), fsName, treeName);
    }

    public boolean entryExist(final String name, final User user) {
        return fs.isEntryExist(name, user.current.getId(), userFsPrefix + user.getId());
    }

    public TFile get(final UUID id, final User user) {
        return fs.getEntry(id, userFsPrefix + user.getId(), treePrefix + user.getId());
    }

    public int rmSelected(final User user) {
        final Map<Long, List<TFile>> byOwner = user.selection.stream()
                .filter(TFile::isRw)
                .collect(Collectors.groupingBy(TFile::getOwner));
        final AtomicInteger counter = new AtomicInteger(0);
        byOwner.forEach((userId, tFiles) -> {
            fs.dropEntries(tFiles.stream().map(TFile::getId).collect(Collectors.toList()), tablePrefix + userId);
            counter.addAndGet(tFiles.size());
        });

        return counter.get();
    }

    public List<TFile> listFolders(final User user) {
        final String fsName = userFsPrefix + user.getId(), treeName = treePrefix + user.getId();

        return fs.listTypedChilds(user.current.getId(), ContentType.DIR, fsName, treeName);
    }

    public List<TFile> getPredictors(final User user) {
        return fs.getPredictors(user.current.getId(), userFsPrefix + user.getId());
    }

    public void updateMetas(final List<TFile> list) {
        list.stream().filter(TFile::isRw).forEach(f -> fs.updateEntry(f.getName(), f.getParentId(), f.getOptions(), f.getId(), f.getOwner(), tablePrefix + f.getOwner()));
    }

    public void changeShareRo(final String shareId, final User user) {
        shared.changeShareRo(shareId, user.getId());
    }

    public void dropShare(final String shareId, final User user) {
        final Share share = shared.selectShare(shareId);
        shared.dropShare(shareId, user.getId());
        shareRemoved(share);
    }

    public boolean noSharesExist(final UUID entryId, final User user) {
        return !shared.isAnyShareExist(entryId, user.getId());
    }

    public List<Share> listShares(final UUID entryId, final User user) {
        return shared.selectSharesByDir(entryId, user.getId());
    }

    public void search(final String rawQuery, final User user) {
        user.searchResults.clear();

        if (isEmpty(rawQuery))
            return;

        user.searchResults.addAll(fs.search("%"+rawQuery.trim()+"%", user.current.getId(), userFsPrefix + user.getId(), treePrefix + user.getId()));
    }

    public boolean isGlobalShareMissed(final UUID entryId, final User user) {
        return shared.selectSharesByDir(entryId, user.getId()) == null;
    }

    public void dropGlobalShareByEntry(final UUID entryId, final User user) {
        shared.dropGlobalShareByEntry(entryId, user.getId());
    }

}

