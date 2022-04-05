package services.impl;

import model.CommandType;
import model.request.*;
import model.user.UDbData;
import play.Logger;
import services.*;
import sql.UserMapper;
import states.AState;
import states.UserState;
import utils.LangMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 03.04.2022 12:36
 * tfs ☭ sweat and blood
 */
@Singleton
public class RouterImpl implements Router {
    private static final Logger.ALogger logger = Logger.of(Router.class);
    private static final Set<CommandType> backTypes = new HashSet<>(Arrays.asList(CommandType.goBack, CommandType.Void, CommandType.cancel));

    @Inject
    private UserMapper userMapper;

    @Inject
    private BotApi api;

    @Inject
    private DataStore store;

    @Override
    public void handle(final TgRequest r) {
        try {
            final UserState backState;
            r.user.backSaver = state -> store.updateUser(state);

            r.user.resolveSaved(userMapper.getUser(r.user.id));
            if (r.user.state() == null) {
                final UDbData data = new UDbData(r.user.id, r.user.getRoot());
                userMapper.insertUser(data);
                r.user.resolveSaved(data);
                backState = null;
            } else {
                if (r instanceof CallbackRequest)
                    if (backTypes.contains(((CallbackRequest) r).getCommand().type))
                        backState = AState._back;
                    else
                        backState = r.user.state().onCallback((CallbackRequest) r, r.user, api, store);
                else if (r instanceof CmdRequest) // reset ?
                    if (((CmdRequest) r).getCmd() == CmdRequest.Cmd.Help) {
                        api.sendText(escapeMd(LangMap.v(r.user.state().helpValue(r.user), r.user.lng)), BotApi.ParseMode.Md2, BotApi.Chat.of(r.user.id), BotApi.helpKbd);
                        return;
                    } else
                        backState = r.user.state().onCommand((CmdRequest) r, r.user, api, store);
                else if (r instanceof FileRequest)
                    backState = r.user.state().onFile((FileRequest) r, r.user, api, store);
                else
                    backState = r.user.state().onText((TextRequest) r, r.user, api, store);
            }

            if (backState == AState._back)
                r.user.backHistory();
            else if (backState != null)
                r.user.addState(backState);

            r.user.state().display(r.user, api, store);

//            userMapper.updateUser(r.user.encodeToSave());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
