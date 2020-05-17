package services;

import model.User;
import model.telegram.api.CallbackAnswer;
import model.telegram.api.TeleFile;
import play.Logger;
import utils.State;

import javax.inject.Inject;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class HeadQuarters {
    private static final Logger.ALogger logger = Logger.of(HeadQuarters.class);

    @Inject
    private TgApi tgApi;

    @Inject
    private GUI gui;

    @Inject
    private FsService fsService;

    @Inject
    private UserService userService;

    public void accept(final User user, final TeleFile file, final String input, final String callbackData, final long msgId, final long callbackId) {
        try {
            if (user.getLastDialogId() > 0) {
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());
                user.setLastDialogId(0);
            }

            if (input != null || file != null)
                tgApi.deleteMessage(msgId, user.getId());

            if (notNull(input).equals("/start")) {
                if (user.getLastMessageId() > 0) {
                    tgApi.deleteMessage(user.getLastMessageId(), user.getId());
                    user.setLastMessageId(0);
                }

                State.freshInit(user, tgApi, gui, userService, fsService);
                user.getState().refreshView();

                return;
            }

            CallbackAnswer answer = null;

            try {
                answer = user.getState().apply(file, input, callbackData);
            } catch (final Exception e) {
                logger.error("Current user state failed to apply: " + e.getMessage(), e);
                State.freshInit(user, tgApi, gui, userService, fsService);
            }

            if (callbackId > 0) {
                if (answer == null)
                    answer = new CallbackAnswer("");

                tgApi.sendCallbackAnswer(notNull(answer.getText()), callbackId, answer.isAlert(), answer.getCacheTimeSeconds());
            }

            user.getState().refreshView();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            if (user.getState() == null || !user.getState().getClass().equals(State.View.class)) {
                logger.error("Hard reset to init View");
                State.freshInit(user, tgApi, gui, userService, fsService);
            }
        } finally {
            userService.updateOpts(user);
        }
    }
}
