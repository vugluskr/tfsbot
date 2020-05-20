package actors;

import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import model.User;
import play.Logger;
import utils.LangMap;
import utils.TFileFactory;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class MkLabelActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(MkLabelActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotInput.class, msg -> doInput(msg.input, msg.user))
                .match(WakeUpNeo.class, msg -> doWakeUp(msg.user))
                .match(GotCallback.class, msg -> switchBack(msg.user))
                .build();
    }

    private void doWakeUp(final User user) {
        tgApi.ask(LangMap.Value.TYPE_LABEL, user, dialogId -> {
            user.setLastDialogId(dialogId);
            saveState(user);
        });
    }

    private void doInput(final String input, final User user) {
        try {
            if (isEmpty(input)) {
                switchBack(user);
                return;
            }

            if (fsService.findAt(input, user.getDirId(), user) != null)
                tgApi.sendPlainText(LangMap.Value.CANT_MKLBL, user, dlgId -> {
                    user.setLastDialogId(dlgId);
                    saveState(user);
                }, input);
            else {
                fsService.upload(TFileFactory.label(input, user.getDirId()), user);
                switchBack(user);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
