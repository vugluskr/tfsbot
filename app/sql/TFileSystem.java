package sql;

import model.TFile;
import model.ContentType;
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

    void createShareView(@Param("viewName") String viewName, @Param("shareId") String shareId, @Param("shareEntryId") String shareEntryId, @Param("shareDirId") String shareDirId,
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

    void dropEntry(@Param("name") String name, @Param("parentId") UUID parentId, @Param("owner") long owner, @Param("viewName") String viewName);

    void updateEntry(@Param("name") String name, @Param("parentId") UUID parentId, @Param("options") int options, @Param("id") UUID id, @Param("owner") long owner,
                     @Param("tableName") String tableName);

    boolean isEntryExist(@Param("name") String name, @Param("parentId") UUID parentId, @Param("viewName") String viewName);

    List<Long> selectServiceWindows(@Param("userId") long userId);

    void deleteServiceWindows(@Param("userId") long userId);

    void addServiceWin(@Param("messageId") long messageId, @Param("userId") long userId);

    void updateLastMessageId(@Param("lastMessageId") long lastMessageId, @Param("userId") long userId);

    void dropLock(@Param("uuid") UUID uuid);

    void createLock(@Param("uuid") UUID uuid, @Param("salt") String salt, @Param("password") String password);

    Map<String, Object> selectEntryPassword(@Param("uuid") UUID uuid);

}
