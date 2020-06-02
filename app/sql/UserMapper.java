package sql;

import model.User;
import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    String selectUser(@Param("id") long id);

    void insertUser(@Param("id") long id, @Param("current") String current);

    void update(@Param("id") long id, @Param("current") String current);

    User selectUserOldWay(@Param("id") long id);
}
