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
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchActor extends StateActor {
    private static final Logger.ALogger logger = Logger.of(SearchActor.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GotCallback.class, this::doCallback)
                .match(GotInput.class, msg -> doSearch(msg.input, msg.user))
                .match(WakeUpNeo.class, msg -> doPrompt(msg.user))
                .build();
    }

    private void doCallback(final GotCallback msg) {
        try {

            final User user = msg.user;

            switch (msg.callbackData) {
                case searchStateInit:
                    switchTo(new WakeUpNeo(user), Strings.Actors.Search);
                    break;
                case gearStateInit:
                    switchTo(new WakeUpNeo(user), Strings.Actors.SearchGear);
                    break;
                case cancel:
                    switchBack(user);
                    break;
                case rewind:
                case forward:
                    user.deltaSearchOffset(msg.callbackData.equals(rewind) ? -10 : 10);
                    tgApi.sendCallbackAnswer(LangMap.Value.PAGE, msg.callbackId, user, (user.getSearchOffset() / 10) + 1);
                    return;
                default:
                    if (!msg.callbackData.startsWith(openEntry)) {
                        switchBack(user);
                        break;
                    }

                    final TFile file = fsService.get(getLong(msg.callbackData), user);
                    if (file == null || file.isLabel()) {
                        switchBack(user);
                        break;
                    }

                    if (file.isDir()) {
                        user.setDirId(file.getId());
                        switchTo(new WakeUpNeo(user), Strings.Actors.View);
                    } else
                        switchTo(new WakeUpNeo(user).with(Strings.Params.fileId, file.getId()), Strings.Actors.OpenFile);

                    break;
            }

            tgApi.sendCallbackAnswer(LangMap.Value.None, msg.callbackId, user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void doPrompt(final User user) {
        try {
            tgApi.sendPlainText(LangMap.Value.TYPE_QUERY, user, dlgId -> {
                user.setLastDialogId(dlgId);
                saveState(user);
            });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doSearch(final String query, final User user) {
        try {
            if (isEmpty(query)) {
                switchBack(user);
                return;
            }

            user.setQuery(query);
            user.setSearchCount(fsService.findChildsByName(user.getDirId(), query.toLowerCase(), user));
            user.setSearchOffset(0);

            final String body;
            if (user.getSearchCount() > 0)
                body = escapeMd(v(LangMap.Value.SEARCHED, user, query, user.getSearchCount()));
            else
                body = escapeMd(v(LangMap.Value.NO_RESULTS, user, query));

            final List<TFile> scope = fsService.getFound(user);
            final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
            upper.add(GUI.Buttons.searchButton);
            upper.add(GUI.Buttons.gearButton);
            upper.add(GUI.Buttons.cancelButton);

            if (user.getSearchOffset() > 0)
                bottom.add(GUI.Buttons.rewindButton);
            if (!scope.isEmpty() && user.getSearchOffset() + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                bottom.add(GUI.Buttons.forwardButton);

            gui.makeMainView(body, scope, user.getSearchOffset(), upper, bottom, user.getLastMessageId(), user, msgId -> {
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
