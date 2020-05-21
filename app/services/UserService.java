package services;

import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.UpdateRef;
import sql.UserMapper;
import utils.Strings;
import utils.TFileFactory;

import javax.inject.Inject;

import static utils.TextUtils.isEmpty;
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

    public User getUser(final UpdateRef update) {
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
            final boolean ru = notNull(cr.getLanguageCode()).equalsIgnoreCase("ru");
            user = new User();
            user.setId(cr.getId());
            user.setLang(cr.getLanguageCode());
            user.setState(Strings.State.View);
            user.setDirId(1);
            fsService.init(user.getId());
            mapper.insertUser(user);
            fsService.mkdir(ru ? "Документы" : "Documents", 1, user.getId());
            fsService.mkdir(ru ? "Фото" : "Photos", 1, user.getId());
            fsService.upload(TFileFactory.label(ru ? "Пример заметки" : "Example note", fsService.mkdir(ru ? "Заметки" : "Notes", 1, user.getId()).getId()), user);
        } else {
            if (isEmpty(db.getState()))
                db.setState(Strings.State.View);

            if (isEmpty(db.getLang()))
                db.setLang(notNull(cr.getLanguageCode(), "ru"));

            return db;
        }

        return user;
    }

    public void update(final User user) {
        if (user.isChanged())
            mapper.update(user);
    }
}
