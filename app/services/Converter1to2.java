package services;

import model.ContentType;
import model.TFile;
import sql.Converter1to2Mapper;
import sql.TFileSystem;
import sql.UserMapper;
import utils.TextUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.06.2020
 * tfs ☭ sweat and blood
 */
@Singleton
public class Converter1to2 {
    private final static String tablePrefix = "fs_data_", userFsPrefix = "fs_user_", pathesTree = "fs_paths_", sharePrefix = "fs_share_";

    private final Converter1to2Mapper mapper;
    private final TFileSystem tfs;
    private final UserMapper um;
    private final TfsService tfsService;

    @Inject
    public Converter1to2(final Converter1to2Mapper mapper, final TFileSystem tfs, final UserMapper users, final TfsService tfsService) throws Exception {
        this.mapper = mapper;
        this.tfs = tfs;
        this.um = users;
        this.tfsService = tfsService;

        doConvert();
    }

    private void doConvert() throws Exception {
        if (!mapper.isOldsExists())
            return;

        mapper.renameOld();
        mapper.createNewUsers();


        final List<Map<String, Object>> users = mapper.getUsers();

        for (final Map<String, Object> user : users) {
            final long userId = (Long) user.get("id");

            tfs.dropView("fs_tree_" + userId);

            final List<Map<String, Object>> fs = mapper.getUserData(userId);
            final Map<Long, UUID> id2uuid = new HashMap<>();

            for (final Map<String, Object> fd : fs)
                id2uuid.put((Long) fd.get("id"), TextUtils.generateUuid());


            final List<TFile> files = new ArrayList<>();


            for (final Map<String, Object> fd : fs) {
                final TFile entry = new TFile();
                entry.setId(id2uuid.get((Long) fd.get("id")));
                entry.setParentId(fd.get("parent_id") == null || (Long) fd.get("parent_id") == 0 ? null : id2uuid.get((Long) fd.get("parent_id")));
                entry.setName((String) fd.get("name"));
                entry.setRefId((String) fd.get("ref_id"));
                entry.setType(ContentType.valueOf((String) fd.get("type")));
                files.add(entry);
            }

            final UUID rootId = files.stream().filter(f -> f.getParentId() == null).findAny().orElse(new TFile()).getId();
            mapper.insertNewUser(userId, rootId, (Long) user.get("last_message"), "{\"entryId\":\""+rootId.toString()+"\",\"offset\":0,\"_class\":\"model.user.DirViewer\"}");
            tfs.createRootTable(tablePrefix + userId);
            tfs.createIndex(tablePrefix + userId, tablePrefix + userId + "_names", "name");
            tfs.createFsView(userFsPrefix + userId, userId, tablePrefix + userId, Collections.emptyList());
            tfs.createFsTree(pathesTree + userId, userFsPrefix + userId);

            for (final TFile f : files)
                tfs.makeEntry(f.getId(), f.getName(), f.getParentId(), f.getType(), f.getRefId(), 0, tablePrefix + userId);

            mapper.dropOldUser(userId);
            mapper.dropOldStruct(userId);
        }

        mapper.dropUsersTable();
    }
}
