package services;

import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.UpdateRef;
import play.libs.Json;
import sql.UserMapper;
import utils.State;
import utils.TFileFactory;
import utils.TextUtils;
import utils.UOpts;

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

    @Inject
    private GUI gui;

    @Inject
    private TgApi tgApi;

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
            final boolean ru;
            if ((ru = TextUtils.notNull(cr.getLanguageCode()).contains("ru")))
                UOpts.Russian.set(user);
            UOpts.Gui.set(user);
            State.freshInit(user, tgApi, gui, this, fsService);
            user.setSavedState(State.stateToJson(user.getState()).toString());
            fsService.init(user.getId());
            mapper.insertUser(user);
            fsService.mkdir(ru ? "Документы" : "Documents", 1, user.getId());
            fsService.mkdir(ru ? "Фото" : "Photos", 1, user.getId());
            fsService.upload(TFileFactory.label(ru ? "Пример заметки" : "Example note", fsService.mkdir(ru ? "Заметки" : "Notes", 1, user.getId()).getId()), user);
        } else {
            if (isEmpty(db.getSavedState()) || !db.getSavedState().startsWith("{"))
                State.freshInit(db, tgApi, gui, this, fsService);
            else
                db.setState(State.stateFromJson(Json.parse(db.getSavedState()), db, tgApi, gui, this, fsService));

            return db;
        }

        return user;
    }

    public void updateOpts(final User user) {
        mapper.updateOpts(user.getLastMessageId(), user.getLastDialogId(), user.getOptions(), State.stateToJson(user.getState()).toString(), user.getId());
    }
}
