package sql;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    Map<String, Object> getUser(@Param("id") long id);

    void insertUser(@Param("id") long id);

    void updateUser(@Param("lastRefId") String lastRefId,
                    @Param("lastText") String lastText,
                    @Param("lastKbd") String lastKeyboard,
                    @Param("data") String data,
                    @Param("id") long id);

    boolean isUserMissed(@Param("id") long id);
}
