package states;

import model.CommandType;
import model.MsgStruct;
import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import play.Logger;
import services.BotApi;
import services.DataStore;
import states.prompts.*;
import utils.AsButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:59
 * tfs ☭ sweat and blood
 */
public abstract class AState implements UserState {
    private static final Logger.ALogger logger = Logger.of(UserState.class);
    public final static UserState _back = new BACKSTATE();

    public static UserState resolve(final String saved, final TgUser user) {
        final int idx;
        if (isEmpty(saved) || (idx = saved.indexOf(';')) < 0)
            return new DirViewer(user.getRoot());

        final String left = saved.substring(idx + 1);

        switch (idx) {
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
        else
            idx = 7;

        return idx + ";" + encode();
    }

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

    protected void doSend(final MsgStruct struct, final TgUser user, final BotApi api) {
        final BotApi.Chat target = BotApi.Chat.of(user.id);

        if (!isEmpty(user.wins)) {
            final List<Long> oldWins = new ArrayList<>(user.wins);

            final Function<Long, CompletionStage<Boolean>> editAction =
                    struct.file != null ? editMsgId -> {
                        final AtomicBoolean succeeded = new AtomicBoolean(true);
                        final CompletableFuture<?>[] chain = new CompletableFuture[3];

                        chain[0] = api.editMedia(struct.file, target, editMsgId)
                                .thenAccept(reply -> {
                                    if (!reply.isOk())
                                        succeeded.set(false);
                                })
                                .exceptionally(e -> {
                                    logger.error("Error edit media: " + e.getMessage(), e);
                                    succeeded.set(false);

                                    return null;
                                }).toCompletableFuture();

                        chain[1] = api.editCaption(struct.caption, struct.mode, target, editMsgId).toCompletableFuture();
                        chain[2] = api.editKeyboard(struct.kbd, target, editMsgId).toCompletableFuture();

                        return CompletableFuture.allOf(chain).thenApply(unused -> succeeded.get());
                    }
                            :
                            editMsgId -> {
                                final AtomicBoolean succeeded = new AtomicBoolean(true);
                                final CompletableFuture<?>[] chain = new CompletableFuture[2];

                                chain[0] = api.editBody(struct.body, struct.mode, target, editMsgId).toCompletableFuture();
                                chain[1] = api.editKeyboard(struct.kbd, target, editMsgId).toCompletableFuture();

                                return CompletableFuture.allOf(chain).thenApply(unused -> succeeded.get());
                            };

            final AtomicLong succeededWin = new AtomicLong(0);
            for (final long win : oldWins) {
                editAction.apply(win)
                        .thenAccept(result -> {
                            if (result)
                                succeededWin.set(win);
                        })
                        .toCompletableFuture().join();

                if (succeededWin.get() > 0) {
                    user.wins.remove(succeededWin.get());

                    user.wins.forEach(w -> CompletableFuture.runAsync(() -> api.dropMessage(target, w)));
                    user.wins = null;
                    user.addWin(succeededWin.get());
                    user.interactionDone();

                    return;
                }
            }

            freshSend(struct, user, target, api);
        } else
            freshSend(struct, user, target, api);
    }

    private void freshSend(final MsgStruct struct, final TgUser user, final BotApi.Chat target, final BotApi api) {
        final CompletionStage<BotApi.Reply> action;
        if (struct.file != null)
            action = api.sendMedia(struct.file, struct.caption, struct.mode, target, struct.kbd);
        else
            action = api.sendText(struct.body, struct.mode, target, struct.kbd);

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
    public UserState onCallback(CallbackRequest request, TgUser user, BotApi api, DataStore store) {return _back;}

    @Override
    public UserState onCommand(CmdRequest request, TgUser user, BotApi api, DataStore store) {return _back;}

    @Override
    public UserState onFile(FileRequest request, TgUser user, BotApi api, DataStore store) {return _back;}

    @Override
    public UserState onText(TextRequest request, TgUser user, BotApi api, DataStore store) {return _back;}

}
