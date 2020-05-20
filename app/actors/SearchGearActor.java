package actors;

import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import play.Logger;
import services.GUI;
import utils.LangMap;
import utils.Strings;

import java.util.ArrayList;
import java.util.List;

import static utils.LangMap.v;
import static utils.Strings.Callback.*;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getLong;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchGearActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(SearchGearActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotCallback.class, msg -> doCallback(msg.callbackId, msg.callbackData, msg.user))
                .match(GotInput.class, msg -> switchTo(msg, Strings.Actors.Search))
                .match(WakeUpNeo.class, msg -> doWake(msg.user))
                .build();
    }

    private void doCallback(final long callbackId, final String callbackData, final User user) {
        try {
            switch (callbackData) {
                case move:
                    tgApi.sendCallbackAnswer(LangMap.Value.None, callbackId, user);
                    switchTo(new WakeUpNeo(user), Strings.Actors.Move);
                    return;
                case drop:
                    tgApi.sendCallbackAnswer(LangMap.Value.DELETED_MANY, callbackId, user, fsService.rmSelected(user));
                    break;
                case cancel:
                    tgApi.sendCallbackAnswer(LangMap.Value.None, callbackId, user);
                    switchBack(user);
                    return;
                case rewind:
                case forward:
                    user.deltaSearchOffset(callbackData.equals(rewind) ? -10 : 10);
                    tgApi.sendCallbackAnswer(LangMap.Value.PAGE, callbackId, user, (user.getSearchOffset() / 10) + 1);
                    break;
                case renameEntry:
                    tgApi.sendCallbackAnswer(LangMap.Value.None, callbackId, user);
                    switchTo(new WakeUpNeo(user), Strings.Actors.Rename);
                    return;
                default:
                    tgApi.sendCallbackAnswer(fsService.setSelected(getLong(callbackData), user) ? LangMap.Value.SELECTED : LangMap.Value.DESELECTED, callbackId, user);
                    break;
            }

            refreshView(user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doWake(final User user) {
        try {
            fsService.resetSelection(user);
            refreshView(user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void refreshView(final User user) {
        final List<TFile> scope = fsService.getFound(user);
        final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);

        final long selection = scope.stream().filter(TFile::isFound).count();
        if (selection > 0) {
            if (selection == 1)
                upper.add(GUI.Buttons.renameButton);

            upper.add(new InlineButton(Strings.Uni.move + "(" + selection + ")", move));
            upper.add(new InlineButton(Strings.Uni.drop + "(" + selection + ")", drop));
        }

        upper.add(GUI.Buttons.cancelButton);

        if (user.getSearchOffset() > 0)
            bottom.add(GUI.Buttons.rewindButton);
        if (!scope.isEmpty() && user.getSearchOffset() + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
            bottom.add(GUI.Buttons.forwardButton);

        gui.makeGearView("_" + escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), user.getSearchCount())) + "_", scope, upper,
                bottom, user.getSearchOffset(), user, msgId -> {
                    if (msgId == 0 || msgId != user.getLastMessageId()) {
                        user.setLastMessageId(msgId);
                        saveState(user);
                    }
                });
    }
}
