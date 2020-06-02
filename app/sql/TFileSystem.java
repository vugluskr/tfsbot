package sql;

import model.TFile;
import model.telegram.ContentType;
import org.apache.ibatis.annotations.Param;

import java.util.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 31.05.2020
 * tfs ☭ sweat and blood
 */
public interface TFileSystem {
    void createRootTable(@Param("tableName") String tableName);

    void createIndex(@Param("tableName") String tableName, @Param("indexName") String indexName, @Param("fields") String fields);

    void createShareView(@Param("viewName") String viewName, @Param("shareId") String shareId, @Param("shareEntryId") UUID shareEntryId, @Param("shareDirId") UUID shareDirId,
                         @Param("shareOwnerTableName") String shareOwnerTableName);

    void createFsView(@Param("viewName") String viewName, @Param("ownerId") long ownerId, @Param("rootTableName") String rootTableName, @Param("shares") List<String> sharedViewsNames);

    void createFsTree(@Param("viewName") String viewName, @Param("consolidatedViewName") String consolidatedViewName);

    void dropView(@Param("viewName") String viewName);

    List<String> selectShareViewsLike(@Param("query") String query);

    void makeEntry(@Param("id") UUID id, @Param("name") String name, @Param("parentId") UUID parentId, @Param("type") ContentType type, @Param("refId") String refId,
                   @Param("options") int options, @Param("tableName") String tableName);

    List<TFile> selectRootDirs(@Param("tableName") String tableName);

    List<TFile> selectSubDirs(@Param("parentId") UUID parentId, @Param("tableName") String tableName);

    boolean isNameBusy(@Param("name") String name, @Param("parentId") UUID parentId, @Param("tableName") String tableName);

    TFile findEntry(@Param("name") String name, @Param("parentId") UUID parentId, @Param("owner") long owner, @Param("viewName") String viewName,
                    @Param("treeViewName") String treeViewName);

    void dropEntry(@Param("name") String name, @Param("parentId") UUID parentId, @Param("owner") long owner, @Param("viewName") String viewName);

    boolean isIndexMissed(@Param("indexName") String indexName);

    boolean isTableMissed(@Param("tableName") String tableName);

    boolean isViewMissed(@Param("viewName") String viewName);

    TFile findRoot(@Param("fsViewName") String fsViewName, @Param("treeViewName") String treeViewName);

    List<TFile> listChilds(@Param("parentId") UUID parentId, @Param("viewName") String viewName, @Param("treeViewName") String treeViewName);

    List<Map<String, Object>> getRawTable(@Param("tableName") String tableName);

    void updateEntry(@Param("name") String name, @Param("parentId") UUID parentId, @Param("options") int options, @Param("id") UUID id, @Param("owner") long owner,
                     @Param("tableName") String tableName);

//    old timers

    void addUuid(@Param("tableName") String tableName);
    void addUuidParent(@Param("tableName") String tableName);

    void update2uuids(@Param("oldId") long oldId, @Param("uuid") UUID uuid, @Param("tableName") String tableName);

    void old0(@Param("uid") long uid);
    void old1(@Param("uid") long uid);
    void old2(@Param("uid") long uid);
    void old3(@Param("uid") long uid);
    void old4(@Param("uid") long uid);
    void old5(@Param("uid") long uid);
    void old6(@Param("uid") long uid);
    void old65(@Param("uid") long uid);
    void old7(@Param("uid") long uid);
    void old8(@Param("uid") long uid);
    void old10(@Param("uid") long uid);
    void old11(@Param("uid") long uid);
    void old12(@Param("uid") long uid);
    void old13(@Param("uid") long uid);
    void old14(@Param("uid") long uid);
    void old15(@Param("uid") long uid);

    boolean isEntryExist(@Param("name") String name, @Param("parentId") UUID parentId, @Param("viewName") String viewName);

    TFile getEntry(@Param("id") UUID id, @Param("viewName") String viewName, @Param("treeViewName") String treeViewName);

    void dropEntries(@Param("ids") List<UUID> ids, @Param("tableName") String tableName);

    List<TFile> listTypedChilds(@Param("id") UUID id, @Param("type") ContentType type, @Param("fsName") String fsName, @Param("treeName") String treeName);

    List<TFile> getPredictors(@Param("id") UUID id, @Param("viewName") String viewName);

    List<TFile> search(@Param("query") String query, @Param("fromDirId") final UUID fromDirId, @Param("viewName") String viewName, @Param("treeViewName") String treeViewName);
}
