package actors;

import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import model.TFile;
import model.User;
import play.Logger;
import utils.LangMap;

import java.util.List;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class RenameActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(RenameActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotInput.class, msg -> doInput(msg.input, msg.user))
                .match(GotCallback.class, msg -> switchBack(msg.user))
                .match(WakeUpNeo.class, msg -> doWake(msg.user))
                .build();
    }

    private void doInput(final String input, final User user) {
        try {
            if (!isEmpty(input)) {
                if (fsService.findAt(input, user.getDirId(), user) != null) {
                    tgApi.sendPlainText(LangMap.Value.CANT_RN_TO, user, dlgId -> {
                        user.setLastDialogId(dlgId);
                        saveState(user);
                    }, input);

                    return;
                }

                final List<TFile> selection = fsService.getSelection(user);

                final TFile file;
                if (!selection.isEmpty() && !(file = selection.get(0)).getName().equals(input)) {
                    file.setName(input);
                    fsService.updateMeta(file, user);
                }
            }

            switchBack(user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doWake(final User user) {
        try {
            final List<TFile> selection = fsService.getSelection(user);
            final TFile file;
            if (selection.isEmpty() || (file = selection.get(0)) == null) {
                switchBack(user);
                return;
            }

            tgApi.ask(LangMap.Value.TYPE_RENAME, user, dlgId -> {
                user.setLastDialogId(dlgId);
                saveState(user);
            }, file.getName());
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
    }
}
