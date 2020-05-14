package services;

import model.TFile;
import model.User;
import model.telegram.api.*;
import play.Logger;
import utils.TextUtils;
import utils.Uni;
import utils.UserMode;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public class GuiService {
    private static final Logger.ALogger logger = Logger.of(GuiService.class);

    @Inject
    private TgApi tgApi;
    @Inject
    private UserService userService;
    @Inject
    private FsService fsService;

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void handle(final UpdateRef input, final User user) {
        try {
            final MessageRef messageRef = input.getMessage() != null ? input.getMessage() : input.getEditedMessage();
            final String text = input.getMessage() != null ? input.getMessage().getText() : input.getEditedMessage() != null ? input.getEditedMessage().getText() : null;
            final String callback = input.getCallback() != null ? input.getCallback().getData() : null;
            final TeleFile file = input.getMessage() != null ? input.getMessage().getTeleFile() : input.getEditedMessage() != null ? input.getEditedMessage().getTeleFile() : null;
            final UserMode mode = UserMode.resolve(user);

            if (messageRef != null)
                CompletableFuture.runAsync(() -> tgApi.deleteMessage(new DeleteMessage(user.getId(), messageRef.getMessageId())));

            if (user.getLastDialogId() > 0) {
                CompletableFuture.runAsync(() -> tgApi.deleteMessage(new DeleteMessage(user.getId(), user.getLastDialogId())));
                user.setLastDialogId(0);
            }

            final CallbackAnswer answer = new CallbackAnswer(input.getCallback() != null ? input.getCallback().getId() : 0, "");

            switch (mode) {
                case MkDirWaitName:
                    user.setMode(0);
                    CompletableFuture.runAsync(() -> userService.updateOpts(user));
                    if (!isEmpty(text)) {
                        if (fsService.findHere(text, user) == null) {
                            fsService.mkdir(text, user.getDirId(), user.getId());
                            answer.setText("Directory '" + text + "' created");
                            doLs(user.getDirId(), user, false);
                        } else {
                            answer.setText("cannot create directory ‘" + text + "’: File exists");
                            answer.setAlert(true);
                        }
                        break;
                    }
                default:
                    if (file != null)
                        CompletableFuture.runAsync(() -> fsService.upload(new TFile(file), user));
                    else if (!isEmpty(callback)) {
                        final long id = TextUtils.getLong(callback);
                        final String cmd = id > 0 ? callback.substring(0, callback.indexOf(String.valueOf(id))) : callback;

                        switch (cmd) {
                            case c.mkDir:
                                user.setMode(UserMode.MkDirWaitName.ordinal());
                                CompletableFuture.runAsync(() -> tgApi.sendMessage(new TextRef("New folder name:", user.getId()).withForcedReply()).thenAccept(apiMessageReply -> {
                                    if (!apiMessageReply.isOk())
                                        return;

                                    user.setLastDialogId(apiMessageReply.getResult().getMessageId());
                                    userService.updateOpts(user);
                                }).thenAccept(aVoid -> tgApi.updateMessage(new UpdateMessage(user.getId(), user.getLastDialogId()).withReply(InlineKeyboard.singleton(new InlineButton(Uni.cancel,
                                        c.cancelDialog))))));
                                break;
                            case c.fullLs:
                                answer.setText("full listing");
                            case c.ls:
                                doLs(id, user, cmd.equals(c.fullLs));
                                break;
                            case c.cd:
                                final TFile dir = fsService.get(id, user);
                                if (dir != null) {
                                    user.setDirId(id);
                                    user.setPwd(dir.getPath());
                                    userService.updatePwd(user);
                                    doLs(id, user, false);
                                    answer.setText("cd " + dir.getPath());
                                }
                                break;
                            case c.get:
                                final TFile data = fsService.get(id, user);
                                if (data != null)
                                    sendMedia(data, user);
                        }
                    } else {
                        user.setLastMessageId(0);
                        doLs(user.getDirId(), user, false);
                    }
            }

            if (answer.getCallbackId() > 0)
                CompletableFuture.runAsync(() -> tgApi.sendCallbackAnswer(answer));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendMedia(final TFile file, final User user) {
/*
        if (user.getLastMessageId() > 0 && file.getType() != ContentType.DIR && file.getType() != ContentType.LABEL && file.getType() != ContentType.STICKER) {
            final EditMedia msg = new EditMedia();
            msg.setChatId(user.getId());
            msg.setMessageId(user.getLastMessageId());
            msg.setMedia(new InputMedia(file.getType().name().toLowerCase(), file.getRefId(), file.getPath()));
            msg.setReplyMarkup(new InlineKeyboard(Collections.singletonList(new ArrayList<InlineButton>(3) {{
                add(new InlineButton(Uni.leftArrow, c.cd + file.getParentId()));
                add(new InlineButton(Uni.rename, c.mv + file.getId()));
                add(new InlineButton(Uni.drop, c.rm + file.getId()));
            }})));

            CompletableFuture.runAsync(() -> tgApi.sendEditMedia(msg)
                    .thenAccept(reply -> {
                        if (reply == null || !reply.isOk()) {
                            user.setLastMessageId(0);
                            userService.updateOpts(user);

                            sendMedia(file, user);
                        }
                    }))
            ;
        } else
*/
            CompletableFuture.runAsync(() -> tgApi.sendFile(file, file.getPath(), new InlineKeyboard(Collections.singletonList(new ArrayList<InlineButton>(3) {{
                add(new InlineButton(Uni.rename, c.mv + file.getId()));
                add(new InlineButton(Uni.drop, c.rm + file.getId()));
                add(new InlineButton(Uni.cancel, c.cancelDialog));
            }})), user.getId()).thenAccept(apiMessageReply -> {
                if (apiMessageReply == null || !apiMessageReply.isOk())
                    return;

                user.setLastDialogId(apiMessageReply.getResult().getMessageId());
                userService.updateOpts(user);
            }));
    }

    private void sendScreen(final TextRef data, final User user) {
        if (user.getLastMessageId() > 0 && user.getLastDialogId() <= 0) {
            final UpdateMessage update = new UpdateMessage();
            update.setChatId(user.getId());
            update.setMessageId(user.getLastMessageId());
            update.setReplyMarkup(data.getReplyMarkup());
            update.setParseMode(data.getParseMode());
            update.setText(data.getText());

            CompletableFuture.runAsync(() -> tgApi.updateMessage(update).thenAccept(reply -> {
                if (reply == null || !reply.isOk()) {
                    user.setLastMessageId(0);
                    userService.updateOpts(user);

                    sendScreen(data, user);
                }
            }));
        } else
            CompletableFuture.runAsync(() -> tgApi.sendMessage(data).thenAccept(apiMessageReply -> {
                if (apiMessageReply == null || !apiMessageReply.isOk())
                    return;

                user.setLastMessageId(apiMessageReply.getResult().getMessageId());
                userService.updateOpts(user);
            }));
    }

    private InlineKeyboard makeLsScreen(final TFile current, final Collection<TFile> listing, final boolean full) {
        final List<List<InlineButton>> kbd = new ArrayList<>();
        final List<InlineButton> headRow = new ArrayList<>();
        headRow.add(new InlineButton(Uni.home, c.cd + "1"));
        if (current.getParentId() > 1) headRow.add(new InlineButton(Uni.leftArrow, c.cd + current.getParentId()));
        headRow.add(new InlineButton(Uni.search, c.search)); // search
        headRow.add(new InlineButton(Uni.insert, c.mkDir));
        headRow.add(new InlineButton(Uni.gear, c.editMode)); // edit
        kbd.add(headRow);

        listing.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .limit(full ? listing.size() : 10)
                .forEach(f -> {
                    final List<InlineButton> row = new ArrayList<>(2);
                    row.add(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? c.cd : c.get) + f.getId()));
                    kbd.add(row);
                });

        if (!full && listing.size() > 10)
            kbd.add(Collections.singletonList(new InlineButton("\u1801 (+" + (listing.size() - 10) + ")", c.fullLs + current.getId())));

        return new InlineKeyboard(kbd);
    }

    private void doLs(final long id, final User user, final boolean full) {
        final TFile file = fsService.get(id, user);

        sendScreen(new TextRef(user.getPwd(), user.getId()).withKeyboard(makeLsScreen(file, fsService.list(id, user), full)), user);
    }

    private interface c {
        String mkDir = "mk";
        String ls = "ls_";
        String cd = "cd_";
        String rm = "rm_";
        String mv = "mv_";
        String get = "gt_";
        String fullLs = "mr_";
        String search = "sr";
        String editMode = "ed";
        String cancelDialog = "cn";
    }
}
