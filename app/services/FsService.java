package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
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
        file.setParentId(owner.getDirId());
        file.setIndate(System.currentTimeMillis());

        mapper.dropEntryByName(file.getName(), file.getParentId(), owner.getId());
        mapper.mkFile(file, owner.getId());
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

    public TFile findHere(final String dir, final User user) { return mapper.findEntryAt(dir, user.getDirId(), user.getId()); }

    public TFile findPath(final String path, final User user) {
        return isEmpty(path) ? null : path.equals("/") ? mapper.getEntry(1, user.getId()) : mapper.findEntryByPath(path, user.getId());
    }

    public void updateMeta(final TFile file, final User user) {
        file.setIndate(System.currentTimeMillis());
        mapper.updateEntry(file.getName(), file.getIndate(), file.getParentId(), file.getId(), user.getId());
    }

    public TFile mkdir(final String name, final long parentId, final long userId) {
        mapper.mkDir(name, parentId, System.currentTimeMillis(), ContentType.DIR.name(), userId);
        return mapper.findEntryAt(name, parentId, userId);
    }

    public List<TFile> list(final User user) {
        return mapper.listEntries(user.getDirId(), user.getId());
    }

    public List<TFile> list(final long dirId, final User user) {
        return mapper.listEntries(dirId, user.getId());
    }

    public TFile getParentOf(final long id, final User user) {
        return mapper.getParentEntry(id, user.getId());
    }

    public List<TFile> findPaths(final Collection<String> paths, final User user) {
        return isEmpty(paths) ? Collections.emptyList() : mapper.findEntriesByPaths(paths.stream().map(p -> p.equals("/") ? "" : p).collect(Collectors.toList()), user.getId());
    }

    public void mkdirs(final Set<TFile> dirs, final User user) {
        dirs.forEach(d -> d.setIndate(System.currentTimeMillis()));
        mapper.mkDirs(dirs, user.getId());
//        mapper.mkDir(name, parentId, System.currentTimeMillis(), ContentType.DIR.name(), userId);

    }

    public void rmByPaths(final List<String> paths, final User user) {
        final List<TFile> find = findPaths(paths, user);

        if (isEmpty(find))
            return;

        rm(find.stream().map(TFile::getId).collect(Collectors.toList()), user);
    }

    public void updateMetas(final List<TFile> toMove, final User user) {
        toMove.forEach(f -> {
            f.setIndate(System.currentTimeMillis());
            mapper.updateEntry(f.getName(), f.getIndate(), f.getParentId(), f.getId(), user.getId());
        });
    }
}
