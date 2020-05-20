package actors;

import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import model.TFile;
import model.User;
import play.Logger;
import utils.LangMap;
import utils.Strings;

import java.util.List;

import static utils.Strings.Callback.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class OpenFileActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(OpenFileActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotInput.class, msg -> switchTo(msg, Strings.Actors.Search))
                .match(GotCallback.class, msg -> doCallback(msg.callbackId, msg.callbackData, msg.user))
                .match(WakeUpNeo.class, msg -> doWake(msg.user))
                .build();
    }

    private void doCallback(final long callbackId, final String callbackData, final User user) {
        try {
            switch (callbackData) {
                case renameEntry:
                    switchTo(new WakeUpNeo(user), Strings.Actors.Rename);
                    break;
                case move:
                    switchTo(new WakeUpNeo(user), Strings.Actors.Move);
                    break;
                case drop:
                    tgApi.sendCallbackAnswer(LangMap.Value.DELETED, callbackId, user);
                    fsService.rmSelected(user);
                    switchBack(user);
                    return;
                default:
                    switchBack(user);
                    break;
            }

            tgApi.sendCallbackAnswer(LangMap.Value.None, callbackId, user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doWake(final User user) {
        try {
            final List<TFile> selection = fsService.getSelection(user);

            if (selection.isEmpty()) {
                switchBack(user);
                return;
            }

            final TFile file = selection.get(0);

            if (file.isDir() || file.isLabel()) {
                switchBack(user);
                return;
            }

            gui.makeFileDialog(file, user.getId(), dlgId -> {
                user.setLastDialogId(dlgId);
                saveState(user);
            });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
