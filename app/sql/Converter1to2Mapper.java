package sql;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.06.2020
 * tfs ☭ sweat and blood
 */
public interface Converter1to2Mapper {
    boolean isOldsExists();

    List<Map<String, Object>> getUsers();

    List<Map<String, Object>> getUserData(@Param("id") long id);

    void dropOldUser(@Param("userId") long userId);

    void dropOldStruct(@Param("userId") long userId);

    void dropUsersTable();

    void insertNewUser(@Param("userId") long userId, @Param("rootId") UUID rootId, @Param("lastMessageId") long lastMessageId, @Param("data") String data);

    void renameOld();

    void createNewUsers();
}
