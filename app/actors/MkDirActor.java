package actors;

import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import model.User;
import play.Logger;
import utils.LangMap;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class MkDirActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(MkDirActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotInput.class, msg -> doInput(msg.input, msg.user))
                .match(GotCallback.class, msg -> switchBack(msg.user))
                .match(WakeUpNeo.class, msg -> doWake(msg.user))
                .build()
                ;
    }

    private void doWake(final User user) {
        try {
            tgApi.ask(LangMap.Value.TYPE_FOLDER, user, dlgId -> {
                user.setLastDialogId(dlgId);
                saveState(user);
            });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doInput(final String input, final User user) {
        try {
            if (isEmpty(input)) return;

            if (fsService.findAt(input, user.getDirId(), user) != null)
                tgApi.sendPlainText(LangMap.Value.CANT_MKDIR, user, dlg -> {
                    user.setLastDialogId(dlg);
                    saveState(user);
                }, input);
            else {
                fsService.mkdir(input, user.getDirId(), user.getId());
                switchBack(user);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
