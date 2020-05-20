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
import utils.Strings.Actors;

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
public class ViewActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(ViewActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotInput.class, msg -> switchTo(msg, Actors.Search))
                .match(WakeUpNeo.class, msg -> {
                    makeView(msg.user);
                    saveState(msg.user);
                })
                .match(GotCallback.class, msg -> {
                    final User user = msg.user;

                    switch (msg.callbackData) {
                        case goUp:
                            if (user.getDirId() > 1) {
                                final TFile dir = fsService.get(user.getDirId(), msg.user);
                                user.setDirId(dir.getParentId());
                                user.setOffset(0);
                                tgApi.sendCallbackAnswer(LangMap.Value.CD, msg.callbackId, msg.user, dir.getPath());
                            }
                            break;
                        case rewind:
                        case forward:
                            user.deltaOffset(msg.callbackData.equals(rewind) ? -10 : 10);
                            tgApi.sendCallbackAnswer(LangMap.Value.PAGE, msg.callbackId, msg.user, (user.getOffset() / 10) + 1);
                            break;
                        case mkLabel:
                            switchTo(new WakeUpNeo(msg.user).with(Strings.Params.dirId, user.getDirId()), Actors.MkLabel);
                            return;
                        case searchStateInit:
                            switchTo(new WakeUpNeo(msg.user), Actors.Search);
                            return;
                        case mkDir:
                            switchTo(new WakeUpNeo(msg.user), Actors.MkDir);
                            return;
                        case gearStateInit:
                            tgApi.sendCallbackAnswer(LangMap.Value.EDIT_MODE, msg.callbackId, msg.user);
                            switchTo(new WakeUpNeo(msg.user).with(Strings.Params.offset, user.getOffset()), Actors.Gear);
                            return;
                        default:
                            if (!msg.callbackData.startsWith(openEntry))
                                return;

                            final long id = getLong(msg.callbackData);

                            if (id <= 0)
                                return;

                            final TFile entry = fsService.get(id, msg.user);
                            if (entry.isDir()) {
                                msg.user.setDirId(id);
                                msg.user.setOffset(0);
                                tgApi.sendCallbackAnswer(LangMap.Value.CD, msg.callbackId, msg.user, entry.getName());
                                break;
                            } else if (entry.isLabel())
                                return;

                            switchTo(new WakeUpNeo(msg.user), Actors.OpenFile);
                            return;
                    }

                    makeView(msg.user);
                    saveState(msg.user);
                })
                .build();
    }

    private void makeView(final User user) {
        try {
            final List<TFile> scope = fsService.list(user.getDirId(), user);
            final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
            if (user.getDirId() > 1) upper.add(GUI.Buttons.goUpButton);
            upper.add(GUI.Buttons.mkLabelButton);
            upper.add(GUI.Buttons.mkDirButton);
            upper.add(GUI.Buttons.searchButton);
            upper.add(GUI.Buttons.gearButton);

            if (user.getOffset() > 0)
                bottom.add(GUI.Buttons.rewindButton);
            if (!scope.isEmpty() && user.getOffset() + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                bottom.add(GUI.Buttons.forwardButton);

            final TFile cd = fsService.get(user.getDirId(), user);

            gui.makeMainView(escapeMd(cd.getPath()) + (scope.isEmpty() ? "\n\n_" + escapeMd(v(LangMap.Value.NO_CONTENT, user)) + "_" : ""), scope, user.getOffset(),
                    upper, bottom, user.getLastMessageId(), user, msgId -> {
                        if (msgId == 0 || msgId != user.getLastMessageId()) {
                            user.setLastMessageId(msgId);
                            saveState(user);
                        }
                    });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
}
