package sql;

import model.User;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    Map<String, Object> getUser(@Param("id") long id);

    void insertUser(@Param("id") long id, @Param("rootId") UUID rootId);

    void updateUser(@Param("lastRefId") String lastRefId,
                    @Param("lastText") String lastText,
                    @Param("lastKbd") String lastKeyboard,
                    @Param("data") String data,
                    @Param("id") long id);
}
