package services;

import model.User;
import sql.UserMapper;

import javax.inject.Inject;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.06.2020
 * tfs ☭ sweat and blood
 */
public class UserService {
    @Inject
    private TfsService tfsService;

    @Inject
    private UserMapper userMapper;

    public User resolveUser(final User preUser) {
        User user = userMapper.getUser(preUser.getId());

        if (user == null) {
            tfsService.initUserTables(preUser.getId());
            userMapper.insertUser(preUser.getId(), tfsService.findRoot(preUser.getId()).getId());

            user = userMapper.getUser(preUser.getId());
        }

        user.lang = notNull(preUser.lang, "en");
        user.name = preUser.name;
        return user;
    }

    public void update(final User user) {
        userMapper.updateUser(
                user.getSubjectId(),
                user.getQuery(),
                user.getSearchDirId(),
                user.getViewOffset(),
                user.getOptions(),
                user.getLastRefId(),
                user.getLastText(),
                user.getLastKeyboard() == null ? null : user.getLastKeyboard().toJson().toString(),
                user.getId());
    }
}
