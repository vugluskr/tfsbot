package sql;

import model.User;
import model.UserAlias;
import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface UserMapper {
    User selectUser(long id);

    void insertUser(User user);

    void updateOffset(User user);

    void updatePwd(User user);

    void insertAlias(@Param("alias") UserAlias alias, @Param("userId") long userId);

    void updateOpts(@Param("mode") int mode, @Param("lastMessageId") long lastMessageId, @Param("lastDialogId") long lastDialogId, @Param("options") int options,
                    @Param("userId") long userId);
}
