package sql;

import model.User;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    User getUser(@Param("id") long id);

    void insertUser(@Param("id") long id, @Param("rootId") UUID rootId);

    void updateUser(@Param("currentDirId") UUID currentDirId, @Param("query") String query, @Param("viewOffset") int viewOffset, @Param("options") int options,
                    @Param("id") long id);

    void selectEntry(@Param("uuid") UUID uuid, @Param("id") long id);

    void deselectEntry(@Param("uuid") UUID uuid, @Param("id") long id);

    void resetSelection(@Param("id") long id);

    void selectEntries(@Param("uuids") Collection<UUID> selection, @Param("id") long id);
}
