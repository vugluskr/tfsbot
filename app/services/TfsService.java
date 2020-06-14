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

    @Transactional
    public void initUserTables(final long userId) {
        fs.createRootTable(tablePrefix + userId);
        fs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");

        fs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());
        fs.createFsTree(pathesTree + userId, userFsPrefix + userId);

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
    public TFile applyShareByLink(final Share share, final User consumer) {
        if (!share.isGlobal())
            return null;

        return applyShare(share, v(LangMap.Value.SHARES_ANONYM, consumer), consumer.getLang(), consumer.getId());
    }

    @Transactional
    public void shareAppliedByProducer(final Share share, final long consumerId, final String langTag, final User producer) {
        if (share.isGlobal())
            return;

        applyShare(share, producer.name, langTag, consumerId);
    }

    private TFile applyShare(final Share share, final String dirName, final String langTag, final long consumerId) {
        final String tableName = tablePrefix + consumerId;
        final List<TFile> rootDirs = fs.selectRootDirs(tableName);

        final TFile sharesDir = rootDirs.stream().filter(TFile::isSharesRoot).findFirst().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setSharesRoot();
            dir.setUnsharable();

            return makeSysDir(v(LangMap.Value.SHARES, langTag), fs.findRoot(userFsPrefix + consumerId, pathesTree + consumerId).getId(), null, dir.getOptions(), consumerId);
        });

        final List<TFile> subs = fs.selectSubDirs(sharesDir.getId(), tableName);

        final TFile shareParentDir = subs.stream().filter(d -> d.isShareFor() && getLong(d.getRefId()) == share.getOwner()).findAny().orElseGet(() -> {
            final TFile dir = new TFile();
            dir.setShareFor(share.getOwner());
            dir.setUnsharable();

            return makeSysDir(dirName, sharesDir.getId(), dir.getRefId(), dir.getOptions(), consumerId);
        });

        fs.createShareView(
                sharePrefix + consumerId + "_" + share.getOwner() + "_" + share.getId(),
                share.getId(),
                share.getEntryId().toString(),
                shareParentDir.getId().toString(),
                tablePrefix + share.getOwner());

        fs.createFsView(userFsPrefix + consumerId, consumerId, tablePrefix + consumerId, fs.selectShareViewsLike(ownerShareViews(consumerId)));

        return shareParentDir;
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

        final String fsName = userFsPrefix + file.getOwner(), treeName = pathesTree + file.getOwner();

        final UUID uuid = generateUuid();
        fs.dropEntry(file.getName(), file.getParentId(), file.getOwner(), fsName);
        fs.makeEntry(uuid, file.getName(), file.getParentId(), file.getType(), file.getRefId(), file.getOptions(), tablePrefix + file.getOwner());

        return fs.getEntry(uuid, fsName, treeName);
    }

    private String shareSharesViews(final String shareId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + shareId;
    }

    private String ownerShareViews(final long ownerId) {
        // имя шары: prefix_КомуВыданаШара_КемВыдана_ИдШары
        return sharePrefix + "%_" + ownerId + "_%";
    }

    private TFile makeSysDir(final String name0, final UUID parentId, final String refId, final int options, final long userId) {
        final String tableName = tablePrefix + userId;
        String name = name0;
        int counter = 1;

        final UUID rootId = fs.findRoot(userFsPrefix + userId, pathesTree + userId).getId();

        while (fs.isNameBusy(name, rootId, tableName))
            name = name0 + " (" + (counter++) + ")";

        fs.makeEntry(generateUuid(), name, parentId, ContentType.DIR, refId, options, tableName);

        return fs.findEntry(name, parentId, userId, userFsPrefix + userId, pathesTree + userId);
    }

    public Share getPublicShare(final String id) {
        return shared.selectPublicShare(id);
    }

    public List<TFile> list(final UUID dirId, final User user) {
        return fs.listChilds(dirId, userFsPrefix + user.getId(), pathesTree + user.getId());
    }

    public boolean entryMissed(final String name, final User user) {
        return !fs.isEntryExist(name, user.getSubjectId(), userFsPrefix + user.getId());
    }

    public TFile get(final UUID id, final User user) {
        return fs.getEntry(id, userFsPrefix + user.getId(), pathesTree + user.getId());
    }

    public void rm(final TFile entry, final User user) {
        fs.dropEntriesByParent(entry.getId(), tablePrefix + entry.getOwner(), userFsPrefix + user.getId());
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

    public List<TFile> search(final User user) {
        if (isEmpty(user.getQuery()))
            return Collections.emptyList();

        return fs.search("%"+user.getQuery().trim()+"%", user.getSearchDirId(), userFsPrefix + user.getId(), pathesTree + user.getId());
    }

    public void dropGlobalShareByEntry(final UUID entryId, final User user) {
        shared.dropGlobalShareByEntry(entryId, user.getId());
    }

    public TFile findRoot(final long userId) {
        return fs.findRoot(userFsPrefix + userId, pathesTree + userId);
    }

    public List<TFile> listTyped(final UUID parentId, final ContentType type, final User user) {
        return fs.listTypedChilds(parentId, type, userFsPrefix + user.getId(), pathesTree + user.getId());
    }
}

