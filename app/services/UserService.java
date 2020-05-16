package services;

import model.Owner;
import model.TFile;
import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.UpdateRef;
import sql.UserMapper;
import utils.UOpts;

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
            user = new User();
            user.setId(cr.getId());
            user.setNick(notNull(cr.getUsername(), "u" + cr.getId()));
            user.setDirId(1);
            user.setPwd("/");
            final boolean ru;
            if ((ru = cr.getLanguageCode().contains("ru")))
                UOpts.Russian.set(user);
            UOpts.Gui.set(user);

            fsService.init(user.getId());
            mapper.insertUser(user);
            fsService.mkdir(ru ? "Документы" : "Documents", 1, user.getId());
            fsService.mkdir(ru ? "Фото" : "Photos", 1, user.getId());
            fsService.upload(TFile.label(ru ? "Пример заметки" : "Example note", fsService.mkdir(ru ? "Заметки" : "Notes", 1, user.getId()).getId()), user);
        } else
            return db;

        return user;
    }

    public <T extends Owner> void updateOpts(final T user) {
        mapper.updateOpts(user.getMode(), user.getLastMessageId(), user.getLastDialogId(), user.getOptions(), user.getOffset(), user.getLastSearch(), user.getId());
    }
}
