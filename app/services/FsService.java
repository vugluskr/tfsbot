package services;

import model.Share;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import sql.FsMapper;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class FsService {
    private static final Logger.ALogger logger = Logger.of(FsService.class);

    @Inject
    private FsMapper mapper;

    @Transactional
    public void init(final long userId) {
        mapper.createUserFs(userId);
        mapper.createRoot(userId);
        mapper.createTree(userId);
    }

    @Transactional
    public void upload(final TFile file, final User owner) {
        try {
            mapper.dropEntryByName(file.getName(), file.getParentId(), owner.getId());
            mapper.mkFile(file, owner.getId());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public TFile get(final long id, final User user) {
        return mapper.getEntry(id, user.getId());
    }

    public TFile findAt(final String name, final long dirId, final User user) { return mapper.findEntryAt(name, dirId, user.getId()); }

    public void updateMeta(final TFile file, final User user) {
        mapper.updateEntry(file.getName(), file.getParentId(), file.getOptions(), file.getId(), user.getId());
    }

    public TFile mkdir(final String name, final long parentId, final long userId) {
        mapper.mkDir(name, parentId, System.currentTimeMillis(), ContentType.DIR.name(), userId);
        return mapper.findEntryAt(name, parentId, userId);
    }

    public List<TFile> list(final long dirId, final User user) {
        return mapper.listEntries(dirId, user.getId());
    }

    public TFile getParentOf(final long id, final User user) {
        return mapper.getParentEntry(id, user.getId());
    }

    public void updateMetas(final List<TFile> toMove, final User user) {
        toMove.forEach(f -> mapper.updateEntry(f.getName(), f.getParentId(), f.getOptions(), f.getId(), user.getId()));
    }

    public List<TFile> listFolders(final long dirId, final User user) {
        return mapper.listTypeEntries(dirId, ContentType.DIR.name(), user.getId());
    }

    public List<TFile> getPredictors(final long dirId, final User user) {
        return mapper.getPredictors(dirId, user.getId());
    }

    @Transactional
    public int findChildsByName(final long dirId, final String text, final User user) {
        mapper.resetFound(user.getId());
        return mapper.selectChildsByName(dirId, "%" + text + "%", user.getId());
    }

    public List<TFile> getFound(final User user) {
        return mapper.selectFound(user.getId());
    }

    public void resetSelection(final User user) {
        mapper.resetSelection(user.getId());
    }

    public int rmSelected(final User user) {
        return mapper.deleteSelected(user.getId());
    }

    public void inversSelection(final long itemId, final User user) {
        mapper.inversSelection(itemId, user.getId());
    }

    public void setExclusiveSelected(final long itemId, final User user) {
        mapper.setExclusiveSelected(itemId, user.getId());
    }

    public List<TFile> getSelection(final User user) {
        return mapper.getSelected(user.getId());
    }

    public void inversListSelection(final User user) {
        mapper.inversListSelection(user.getDirId(), user.getId());
    }

    public void inversFoundSelection(final User user) {
        mapper.inversFoundSelection(user.getId());
    }

    public List<Share> listShares(final long entryId, final User user) {
        return mapper.selectSharesByDir(entryId, user.getId());
    }

    @Transactional
    public Share getCreateLinkShare(final User user) {
        final Share exist = mapper.getLinkShare(user.getDirId(), user.getId());

        if (exist != null)
            return exist;

        final char[] uuid = TextUtils.generateUuid().toString().replace("-", "").toCharArray();
        String tmp = new String(uuid);

        for (int i = 6; i < uuid.length; i++) {
            tmp = new String(Arrays.copyOfRange(uuid, 0, i));

            if (mapper.isShareIdAvailable(tmp))
                break;
        }

        final Share share = new Share();
        share.setName(mapper.getEntry(user.getDirId(), user.getId()).getName());
        share.setId(tmp);
        share.setOwner(user.getId());
        share.setEntryId(user.getDirId());

        mapper.insertShare(share);

        return share;
    }

    @Transactional
    public void dropPublicLink(final User user) {
        mapper.deletePublicShare(user.getDirId(), user.getId());
        final TFile dir = mapper.getEntry(user.getDirId(), user.getId());

        if (mapper.countSharesByDir(user.getDirId()) == 0 && dir.isShared()) {
            dir.setUnshared();
            updateMeta(dir, user);
        }
    }

    @Transactional
    public void validatePublicLink(final User user) {
        final TFile dir = mapper.getEntry(user.getDirId(), user.getId());

        if (!dir.isShared()) {
            dir.setShared();
            updateMeta(dir, user);
        }
    }

    @Transactional
    public void dropPubLinkPassword(final User user) {
        final Share share = mapper.getLinkShare(user.getDirId(), user.getId());

        if (share == null)
            dropPublicLink(user);
        else {
            share.clearPasswordLock();
            mapper.updateShare(share);
        }
    }

    public void dropPubLinkVailid(final User user) {
        final Share share = mapper.getLinkShare(user.getDirId(), user.getId());

        if (share == null)
            dropPublicLink(user);
        else {
            share.clearValids();
            mapper.updateShare(share);
        }
    }

    public void markGlobForPassEdit(final User user) {
        final List<Share> mine = mapper.selectSharesByDir(user.getDirId(), user.getId());
        final Share share = mine.stream().filter(s -> s.getSharedTo() == 0).findAny().orElse(null);

        if (share == null)
            dropPublicLink(user);
        else {
            mine.stream().filter(s -> s.getSharedTo() > 0 && s.isEdited()).forEach(s -> {
                s.clearEdited();
                mapper.updateShare(s);
            });
            share.setEdited();
            share.setHash(null);
            share.setSalt(null);
            mapper.updateShare(share);
        }
    }

    public Share getEdited(final User user) {
        return mapper.selectSharesByDir(user.getDirId(), user.getId()).stream().filter(Share::isEdited).findAny().orElse(new Share());
    }

    public void updateShare(final Share share) {
        mapper.updateShare(share);
    }

    public void clearEditedShares(final User user) {
        final List<Share> mine = mapper.selectSharesByDir(user.getDirId(), user.getId());
        mine.stream().filter(s -> s.getSharedTo() > 0 && s.isEdited()).forEach(s -> {
            s.clearEdited();
            mapper.updateShare(s);
        });
    }
}
