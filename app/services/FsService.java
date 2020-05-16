package services;

import model.Owner;
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
import java.util.stream.Collectors;

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

    public void upload(final TFile file, final Owner owner) {
        try {
            file.setParentId(owner.getDirId());
            file.setIndate(System.currentTimeMillis());

            mapper.dropEntryByName(file.getName(), file.getParentId(), owner.getId());
            mapper.mkFile(file, owner.getId());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void rm(final long id, final Owner owner) {
        if (id <= 1)
            return;

        mapper.dropEntry(id, owner.getId());
        mapper.dropOrphans(id, owner.getId());
    }

    public void rm(final Collection<Long> ids, final Owner owner) {
        if (isEmpty(ids))
            return;

        if (ids.size() == 1) {
            rm(ids.iterator().next(), owner);
            return;
        }

        mapper.dropEntries(ids, owner.getId());
        mapper.dropMultiOrphans(ids, owner.getId());
    }

    public TFile get(final long id, final Owner user) {
        return mapper.getEntry(id, user.getId());
    }

    public TFile findHere(final String dir, final Owner user) { return mapper.findEntryAt(dir, user.getDirId(), user.getId()); }

    public TFile findPath(final String path, final Owner user) {
        return isEmpty(path) ? null : path.equals("/") ? mapper.getEntry(1, user.getId()) : mapper.findEntryByPath(path, user.getId());
    }

    public void updateMeta(final TFile file, final Owner user) {
        file.setIndate(System.currentTimeMillis());
        mapper.updateEntry(file.getName(), file.getIndate(), file.getParentId(), file.isSelected(), file.getId(), user.getId());
    }

    public TFile mkdir(final String name, final long parentId, final long userId) {
        mapper.mkDir(name, parentId, System.currentTimeMillis(), ContentType.DIR.name(), userId);
        return mapper.findEntryAt(name, parentId, userId);
    }

    public List<TFile> list(final Owner user) {
        return mapper.listEntries(user.getDirId(), user.getId());
    }

    public List<TFile> list(final long dirId, final Owner user) {
        return mapper.listEntries(dirId, user.getId());
    }

    public TFile getParentOf(final long id, final Owner user) {
        return mapper.getParentEntry(id, user.getId());
    }

    public List<TFile> findPaths(final Collection<String> paths, final Owner user) {
        return isEmpty(paths) ? Collections.emptyList() : mapper.findEntriesByPaths(paths.stream().map(p -> p.equals("/") ? "" : p).collect(Collectors.toList()), user.getId());
    }

    public void mkdirs(final Set<TFile> dirs, final Owner user) {
        dirs.forEach(d -> d.setIndate(System.currentTimeMillis()));
        mapper.mkDirs(dirs, user.getId());
    }

    public void updateMetas(final List<TFile> toMove, final Owner user) {
        toMove.forEach(f -> {
            f.setIndate(System.currentTimeMillis());
            mapper.updateEntry(f.getName(), f.getIndate(), f.getParentId(), f.isSelected(), f.getId(), user.getId());
        });
    }

    public List<TFile> listFolders(final long dirId, final Owner user) {
        return mapper.listTypeEntries(dirId, ContentType.DIR.name(), user.getId());
    }

    public List<TFile> getByIds(final Set<Long> ids, final Owner user) {
        return isEmpty(ids) ? Collections.emptyList() : mapper.getByIds(ids, user.getId());
    }

    public List<TFile> getPredictors(final long dirId, final Owner user) {
        return mapper.getPredictors(dirId, user.getId());
    }

    @Transactional
    public int findChildsByName(final String text, final Owner user) {
        mapper.resetFound(user.getId());
        return mapper.selectChildsByName(user.getDirId(), "%" + text + "%", user.getId());
    }

    public TFile getSelectionSingle(final Owner user) {
        return mapper.getSelectedSingle(user.getId());
    }

    @Transactional
    public void newSelection(final Collection<Long> selection, final Owner user) {
        mapper.resetSelection(user.getId());
        if (!isEmpty(selection))
            mapper.setSelection(selection, true, user.getId());
    }

    public List<TFile> getSelection(final Owner user) {
        return mapper.getSelected(user.getId());
    }

    public int rmSelected(final Owner user) {
        return mapper.dropSelectionWithChilds(user.getId());
    }

    public void setSelected(final Collection<Long> selection, final Owner user) {
        mapper.setSelection(selection, true, user.getId());
    }

    public void setDeselected(final Collection<Long> selection, final Owner user) {
        mapper.setSelection(selection, false, user.getId());
    }

    public List<TFile> getFound(final User user) {
        return mapper.selectFound(user.getId());
    }
}
