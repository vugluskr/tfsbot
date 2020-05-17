package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import org.mybatis.guice.transactional.Transactional;
import play.Logger;
import sql.FsMapper;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class FsService {
    private static final Logger.ALogger logger = Logger.of(FsService.class);

    @Inject
    private FsMapper mapper;

    public void init(final long userId) {
        if (!mapper.isFsTableExist(userId)) {
            mapper.createUserFs(userId);
            mapper.createRoot(userId);
        }

        mapper.createTree(userId);
    }

    public void upload(final TFile file, final User owner) {
        try {
            mapper.dropEntryByName(file.getName(), file.getParentId(), owner.getId());
            mapper.mkFile(file, owner.getId());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void rm(final long id, final User owner) {
        if (id <= 1)
            return;

        mapper.dropEntry(id, owner.getId());
        mapper.dropOrphans(id, owner.getId());
    }

    public void rm(final Collection<Long> ids, final User owner) {
        if (isEmpty(ids))
            return;

        if (ids.size() == 1) {
            rm(ids.iterator().next(), owner);
            return;
        }

        mapper.dropEntries(ids, owner.getId());
        mapper.dropMultiOrphans(ids, owner.getId());
    }

    public TFile get(final long id, final User user) {
        return mapper.getEntry(id, user.getId());
    }

    public TFile findAt(final String name, final long dirId, final User user) { return mapper.findEntryAt(name, dirId, user.getId()); }

    public void updateMeta(final TFile file, final User user) {
        mapper.updateEntry(file.getName(), file.getParentId(), file.getId(), user.getId());
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
        toMove.forEach(f -> mapper.updateEntry(f.getName(), f.getParentId(), f.getId(), user.getId()));
    }

    public List<TFile> listFolders(final long dirId, final User user) {
        return mapper.listTypeEntries(dirId, ContentType.DIR.name(), user.getId());
    }

    public List<TFile> getByIds(final Set<Long> ids, final User user) {
        return isEmpty(ids) ? Collections.emptyList() : mapper.getByIds(ids, user.getId());
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
}
