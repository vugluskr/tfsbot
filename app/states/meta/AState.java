package states.meta;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.TUser;
import play.Logger;
import services.BotApi;
import services.DataStore;
import states.*;
import states.prompts.*;
import utils.AsButton;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:59
 * tfs ☭ sweat and blood
 */
public abstract class AState implements UserState {
    private static final Logger.ALogger logger = Logger.of(UserState.class);
    public final static UserState _back = new FAKESTATE();
    public final static UserState _hold = new FAKESTATE();

    protected UUID entryId;
    protected TFile entry;

    public static UserState resolve(final String saved, final TUser user) {
        final int idx;
        if (isEmpty(saved) || (idx = saved.indexOf(';')) < 0)
            return new DirViewer(user.getRoot());

        final String left = saved.substring(idx + 1);

        switch (getInt(saved.substring(0, idx))) {
            case 0:
                return new DirMaker(left);
            case 1:
                return new DropConfirmer(left);
            case 2:
                return new LabelMaker(left);
            case 3:
                return new Locker(left);
            case 4:
                return new Renamer(left);
            case 5:
                return new Unlocker(left);
            case 6:
                return new DirGearer(left);
            case 8:
                return new EntrySharer(left);
            case 9:
                return new EntrySharesGearer(left);
            case 10:
                return new EntrySharesGranter(left);
            case 11:
                return new FileViewer(left);
            case 12:
                return new LabelViewer(left);
            case 13:
                return new Searcher(left);
            case 14:
                return new LabelEditor(left);
            case 15:
                return new OpdsSearcher(left);
            default:
                return new DirViewer(left);
        }
    }

    @Override
    public final String save() {
        final int idx;
        if (this instanceof DirMaker)
            idx = 0;
        else if (this instanceof DropConfirmer)
            idx = 1;
        else if (this instanceof LabelMaker)
            idx = 2;
        else if (this instanceof Locker)
            idx = 3;
        else if (this instanceof Renamer)
            idx = 4;
        else if (this instanceof Unlocker)
            idx = 5;
        else if (this instanceof DirGearer)
            idx = 6;
        else if (this instanceof EntrySharer)
            idx = 8;
        else if (this instanceof EntrySharesGearer)
            idx = 9;
        else if (this instanceof EntrySharesGranter)
            idx = 10;
        else if (this instanceof FileViewer)
            idx = 11;
        else if (this instanceof LabelViewer)
            idx = 12;
        else if (this instanceof Searcher)
            idx = 13;
        else if (this instanceof LabelEditor)
            idx = 14;
        else if (this instanceof OpdsSearcher)
            idx = 15;
        else
            idx = 7;

        return idx + ";" + encode();
    }

    protected abstract String encode();

    protected void pagedList(final List<? extends AsButton> scope, final int count, final int offset, final MsgStruct struct) {
        pagedList(scope, count, offset, struct, AsButton::toButton);
    }

    protected <T> void pagedList(final List<T> scope, final int count, final int offset, final MsgStruct struct, final BiFunction<T, Integer, BotApi.Button> buttoner) {
        for (int i = 0; i < scope.size(); i++)
            struct.kbd.newLine().button(buttoner.apply(scope.get(i), i));

        if (offset > 0 || count > 10) {
            struct.kbd.newLine();

            if (offset > 0)
                struct.kbd.button(CommandType.rewind.b());
            if ((count - offset) > 10)
                struct.kbd.button(CommandType.forward.b());
        }
    }

    @Override
    public final void doSend(final MsgStruct struct, final TUser user, final BotApi api) {
        doSend(struct, user, api, false);
    }

    @Override
    public final void doSend(final MsgStruct struct, final TUser user, final BotApi api, final boolean forceNew) {
        if (forceNew || isEmpty(user.wins)) {
            freshSend(struct, user, api);
            return;
        }

        final long winId = user.wins.first();

        final CompletionStage<Boolean> sendAction = isEmpty(struct.caption)
                ? api.editText(new BotApi.TextMessage(struct.body, struct.mode, struct.kbd, user.id), winId)
                : api.editMedia(new BotApi.MediaMessage(struct.file, struct.caption, struct.mode, struct.kbd, user.id), winId);

        CompletableFuture.runAsync(() -> sendAction
                .thenAccept(success -> {
                    if (success) {
                        user.interactionDone();
                        return;
                    }

                    user.wins.remove(winId);
                    user.interactionDone();
                    CompletableFuture.runAsync(() -> api.dropMessage(winId, user.id));
                    doSend(struct, user, api);
                }));
    }

    private void freshSend(final MsgStruct struct, final TUser user, final BotApi api) {
        final CompletionStage<BotApi.Reply> action;
        if (struct.rawFile != null)
            action = doBookUpload(struct, user, api);
        else if (struct.file != null)
            action = api.sendMedia(new BotApi.MediaMessage(struct.file, struct.caption, struct.mode, struct.kbd, user.id));
        else
            action = api.sendText(new BotApi.TextMessage(struct.body, struct.mode, struct.kbd, user.id));

        CompletableFuture.runAsync(() -> action
                .thenAccept(reply -> {
                    if (reply.isOk() && reply.getMsgId() > 0) {
                        user.addWin(reply.getMsgId());
                        user.interactionDone();
                    }
                })
                .exceptionally(throwable -> {
                    logger.error(throwable.getMessage(), throwable);
                    return null;
                }));
    }

    @Override
    public CompletionStage<BotApi.Reply> doBookUpload(final MsgStruct struct, final TUser user, final BotApi api) {
        return api.sendMedia(new BotApi.MediaMessage(
                        struct.file,
                        new BotApi.RawMedia(struct.rawFile, struct.rawFile.getName().contains("fb2") ? "application/fb2+zip" : "application/epub"),
                        struct.caption,
                        struct.mode,
                        struct.kbd,
                        user.id
                ))
                .thenApply(reply -> {
                    if (reply.isOk() && reply.getMsgId() > 0) {
                        user.addWin(reply.getMsgId());
                        user.interactionDone();
                    }

                    return reply;
                });
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        api.sendReaction(new BotApi.ReactionMessage(request.queryId, "", user.id));

        return voidOnCallback(request, user, api, store);
    }

    protected UserState voidOnCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        return _back;
    }

    @Override
    public UserState onCommand(final CmdRequest request, final TUser user, final BotApi api, final DataStore store) {return _back;}

    @Override
    public UserState onFile(final FileRequest request, final TUser user, final BotApi api, final DataStore store) {return _back;}

    @Override
    public UserState onText(final TextRequest request, final TUser user, final BotApi api, final DataStore store) {return _back;}

    @Override
    public final UUID entryId() {
        return entryId;
    }
}
