package services;

import model.User;
import model.UserAlias;
import model.telegram.api.ContactRef;
import model.telegram.api.UpdateRef;
import sql.UserMapper;

import javax.inject.Inject;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class UserService {
    @Inject
    private UserMapper mapper;

    @Inject
    private FsService fsService;

    public User getContact(final UpdateRef update) {
        final ContactRef cr;
        if (update.getMessage() != null)
            cr = update.getMessage().getContactRef();
        else if (update.getCallback() != null)
            cr = update.getCallback().getFrom();
        else
            cr = update.getEditedMessage().getContactRef();

        final User db = mapper.selectUser(cr.getId());
        final User user;

        if (db == null) {
            user = new User();
            user.setId(cr.getId());
            user.setNick(notNull(cr.getUsername(), "u" + cr.getId()));
            user.setDirId(1);
            user.setPwd("/");

            fsService.init(user.getId());
            mapper.insertUser(user);
        } else
            return db;

        return user;
    }

    public void updatePwd(final User user) {
        mapper.updatePwd(user);
    }

    public void insertAlias(final UserAlias alias, final User user) {
        mapper.insertAlias(alias, user.getId());
    }
}
