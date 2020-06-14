package sql;

import model.User;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    User getUser(@Param("id") long id);

    void insertUser(@Param("id") long id, @Param("rootId") UUID rootId);

    void updateUser(@Param("subjectId") UUID subjectId,
                    @Param("query") String query,
                    @Param("searchDirId") UUID searchDirId,
                    @Param("viewOffset") int viewOffset,
                    @Param("options") int options,
                    @Param("lastRefId") String lastRefId,
                    @Param("lastText") String lastText,
                    @Param("lastKbd") String lastKeyboard,
                    @Param("id") long id);
}
