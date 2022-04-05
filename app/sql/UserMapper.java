package sql;

import model.user.UDbData;
import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    UDbData getUser(@Param("id") long id);

    void insertUser(@Param("u") UDbData u);

    void updateUser(@Param("u") UDbData u);
}
