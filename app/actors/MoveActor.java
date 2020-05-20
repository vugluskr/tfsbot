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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static utils.Strings.Callback.*;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getLong;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class MoveActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(MoveActor.class);

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
                case rewind:
                case forward:
                    user.deltaOffset(callbackData.equals(rewind) ? -10 : 10);
                    tgApi.sendCallbackAnswer(LangMap.Value.PAGE, callbackId, user, (user.getOffset() / 10) + 1);
                    break;
                case goUp:
                    goTo(fsService.getParentOf(user.getDirId(), user), callbackId, user);
                    break;
                case put:
                    final List<TFile> selection = fsService.getSelection(user);
                    final Set<Long> predictors = fsService.getPredictors(user.getDirId(), user).stream().map(TFile::getId).collect(Collectors.toSet());
                    final AtomicInteger counter = new AtomicInteger(0);
                    selection.stream().filter(f -> !f.isDir() || !predictors.contains(f.getId())).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(user.getDirId()));
                    fsService.updateMetas(selection, user);
                    fsService.resetSelection(user);
                    tgApi.sendCallbackAnswer(LangMap.Value.MOVED, callbackId, user, counter.get());
                    switchTo(new WakeUpNeo(user), Strings.Actors.View);
                    return;
                case cancel:
                    tgApi.sendCallbackAnswer(LangMap.Value.None, callbackId, user);
                    switchBack(user);
                    return;
                default:
                    goTo(fsService.get(getLong(callbackData), user), callbackId, user);
                    break;
            }

            refreshView(user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void goTo(final TFile dir, final long callbackId, final User user) {
        tgApi.sendCallbackAnswer(LangMap.Value.CD, callbackId, user, dir.getName());
        user.setDirId(dir.getId());
        saveState(user);
    }

    private void doWake(final User user) {
        try {
            user.setOffset(0);
            refreshView(user);
            saveState(user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void refreshView(final User user) {
        final List<TFile> scope = fsService.listFolders(user.getDirId(), user);
        final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
        if (user.getDirId() > 1)
            upper.add(GUI.Buttons.goUpButton);
        upper.add(GUI.Buttons.putButton);
        upper.add(GUI.Buttons.cancelButton);

        if (user.getOffset() > 0)
            bottom.add(GUI.Buttons.rewindButton);
        if (!scope.isEmpty() && user.getOffset() + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
            bottom.add(GUI.Buttons.forwardButton);

        final TFile cd = fsService.get(user.getDirId(), user);

        gui.makeMovingView(escapeMd(cd.getPath()), scope, upper, bottom, user.getOffset(), user, msgId -> {
            if (msgId == 0 || msgId != user.getLastMessageId()) {
                user.setLastMessageId(msgId);
                saveState(user);
            }
        });
    }
}
