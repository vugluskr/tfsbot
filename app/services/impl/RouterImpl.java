package services.impl;

import model.CommandType;
import model.MsgStruct;
import model.Share;
import model.TFile;
import model.request.*;
import model.user.TgUser;
import model.user.UDbData;
import play.Logger;
import play.libs.Json;
import services.BotApi;
import services.DataStore;
import services.Router;
import sql.UserMapper;
import states.DirViewer;
import states.FileViewer;
import states.meta.AState;
import states.OpdsSearcher;
import states.meta.UserState;
import utils.LangMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static states.meta.AState._hold;
import static utils.TextUtils.isEmpty;

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
            UserState backState = null;
            r.user.asyncSaver = state -> store.updateUser(state);

            try {
                r.user.resolveSaved(userMapper.getUser(r.user.id));
            } catch (final Exception e) {
                r.user.resetState();
                r.user.addState(new DirViewer(store.findRootId(r.user.id)));
            }
            if (r.user.state() == null) {
                final UDbData data = new UDbData(r.user.id, store.reinitUserTables(r.user.id));
                if (userMapper.getUser(r.user.id) == null)
                    userMapper.insertUser(data);
                else
                    userMapper.updateUser(data);
                r.user.resolveSaved(data);
            } else {
                if (r instanceof CallbackRequest)
                    if (backTypes.contains(((CallbackRequest) r).getCommand().type))
                        backState = AState._back;
                    else if (((CallbackRequest) r).getCommand().type == CommandType.justCloseCmd) {
                        api.sendReaction(new BotApi.ReactionMessage(((CallbackRequest) r).queryId, "", r.user.id));
                    } else
                        backState = r.user.state().onCallback((CallbackRequest) r, r.user, api, store);
                else if (r instanceof CmdRequest) {
                    if (((CmdRequest) r).getCmd() == CmdRequest.Cmd.Help) {
                        final MsgStruct struct = new MsgStruct();
                        struct.body = LangMap.v(r.user.state().helpValue(r.user), r.user.lng);
                        struct.kbd = BotApi.helpKbd;
                        struct.mode = BotApi.ParseMode.Md2;

                        r.user.state().doSend(struct, r.user, api);
                        return;
                    }

                    if (((CmdRequest) r).getCmd() == CmdRequest.Cmd.Reset) {
                        r.user.resetState();
                        store.reinitUserTables(r.user.id);
                        store.dropUserShares(r.user.id);
                    } else if (((CmdRequest) r).getCmd() == CmdRequest.Cmd.JoinShare) {
                        final Share share = store.getPublicShare(((CmdRequest) r).getArg());

                        if (share != null) {
                            final TFile f = store.applyShareByLink(share, r.user);

                            if (f != null)
                                store.buildHistoryTo(f.getId(), r.user);
                        }
                    } else if (((CmdRequest) r).getCmd() == CmdRequest.Cmd.FbSearch) {
                        r.user.resetOpds();
                        if (r.user.getBookStore() == null) {
                            api.sendText(new BotApi.TextMessage(
                                    LangMap.v(LangMap.Value.NO_BOOKSTORE, r.user.lng),
                                    BotApi.ParseMode.Md2,
                                    BotApi.helpKbd,
                                    r.user.id
                            ));
                        } else if (!isEmpty(((CmdRequest) r).getArg()))
                            backState = new OpdsSearcher(r.user.state().entryId(), ((CmdRequest) r).getArg());
                    } else
                        backState = r.user.state().onCommand((CmdRequest) r, r.user, api, store);
                } else if (r instanceof FileRequest) {
                    if (((FileRequest) r).isFb2()) {
                        CompletableFuture.runAsync(() ->
                                api.downloadFile(((FileRequest) r).getFile().refId)
                                        .thenApply(bytes -> store.getAndParseFb(bytes, ((FileRequest) r).getFile(), api, r.user))
                                        .thenAccept(uuid -> {
                                            if (uuid == null)
                                                handleAfterAction(r.user.state().onFile((FileRequest) r, r.user, api, store), r.user);
                                            else {
                                                store.buildHistoryTo(uuid, r.user);
                                                r.user.state().display(r.user, api, store);
                                            }
                                        }));

                        backState = _hold;
                    } else
                        backState = r.user.state().onFile((FileRequest) r, r.user, api, store);
                } else
                    backState = r.user.state().onText((TextRequest) r, r.user, api, store);
            }

            handleAfterAction(backState, r.user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleAfterAction(final UserState backState, final TgUser user) {
        if (backState == AState._back)
            user.backHistory();
        else if (backState != null && backState != _hold)
            user.addState(backState);

        if (backState != _hold)
            user.state().display(user, api, store);
    }
}
