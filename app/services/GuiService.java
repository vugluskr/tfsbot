package services;

import model.TFile;
import model.User;
import model.telegram.Request;
import model.telegram.api.InlineButton;
import model.telegram.api.InlineKeyboard;
import model.telegram.api.TextRef;
import model.telegram.api.UpdateRef;
import play.Logger;
import utils.CallCmd;
import utils.UOpts;
import utils.Uni;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.CallCmd.fullLs;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public class GuiService {
    private static final Logger.ALogger logger = Logger.of(GuiService.class);

    @Inject
    private TgApi2 tgApi;
    @Inject
    private UserService userService;
    @Inject
    private FsService fsService;

    public void handle(final UpdateRef input, final User user) {
        final AtomicBoolean updateOpts = new AtomicBoolean(false);

        try {
            final Request request = new Request(input);
            if (request.id > 0)
                tgApi.deleteMessage(request.id, user.getId());

            if (user.getLastDialogId() > 0) {
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());
                user.setLastDialogId(0);
                updateOpts.set(true);
            }

            if (request.file != null)
                fsService.upload(new TFile(request.file), user);

            if (UOpts.WaitFolderName.is(user)) {
                UOpts.WaitFolderName.clear(user);
                updateOpts.set(true);

                if (!isEmpty(request.text)) {
                    if (fsService.findHere(request.text, user) == null) {
                        fsService.mkdir(request.text, user.getDirId(), user.getId());
                        doLs(user.getDirId(), user, false);
                    } else
                        tgApi.sendPlainText("cannot create directory ‘" + request.text + "’: File exists", user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            }

            if (request.isCallback()) {
                final StringBuilder callAnswer = new StringBuilder();

                switch (request.callbackCmd) {
                    case mkDir:
                        UOpts.WaitFolderName.set(user);
                        updateOpts.set(true);

                        tgApi.ask("New folder name:", user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });
                        break;
                    case fullLs:
                    case ls:
                        doLs(request.callbackId, user, request.callbackCmd == fullLs);
                        break;
                    case cd:
                        final TFile dir = fsService.get(request.callbackId, user);
                        if (dir != null) {
                            callAnswer.append("cd ").append(dir.getPath());

                            user.setDirId(request.callbackId);
                            user.setPwd(dir.getPath());
                            userService.updatePwd(user);
                            doLs(request.callbackId, user, false);
                        }
                        break;
                    case get:
                        final TFile file = fsService.get(request.callbackId, user);
                        if (file != null)
                            tgApi.sendMedia(file, file.getPath(), new InlineKeyboard(Collections.singletonList(new ArrayList<InlineButton>(3) {{
                                add(new InlineButton(Uni.leftArrow, CallCmd.cd.of(file.getParentId())));
                                add(new InlineButton(Uni.rename, CallCmd.rename.of(file.getId())));
                                add(new InlineButton(Uni.move, CallCmd.mv.of(file.getId())));
                                add(new InlineButton(Uni.drop, CallCmd.rm.of(file.getId())));
                            }})), user.getId(), dialogId -> {
                                user.setLastDialogId(dialogId);
                                userService.updateOpts(user);
                            });
                }

                tgApi.sendCallbackAnswer(callAnswer.toString(), request.callbackReplyId);
            } else {
                if (user.getLastMessageId() > 0) {
                    user.setLastMessageId(0);
                    updateOpts.set(true);
                }
                doLs(user.getDirId(), user, false);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (updateOpts.get())
                userService.updateOpts(user);
        }
    }

    private void sendScreen(final TextRef data, final User user) {
        tgApi.sendOrUpdate(data.getText(), data.getParseMode(), data.getReplyMarkup(), user.getLastMessageId(), user.getId(), msgId -> {
            if (msgId == 0 || msgId != user.getLastMessageId()) {
                user.setLastMessageId(msgId);
                userService.updateOpts(user);
            }
        });
    }

    private InlineKeyboard makeLsScreen(final TFile current, final Collection<TFile> listing, final boolean full) {
        final List<List<InlineButton>> kbd = new ArrayList<>();
        final List<InlineButton> headRow = new ArrayList<>();
        headRow.add(new InlineButton(Uni.home, CallCmd.cd.of(1L)));
        if (current.getParentId() > 1) headRow.add(new InlineButton(Uni.leftArrow, CallCmd.cd.of(current.getParentId())));
        headRow.add(new InlineButton(Uni.search, CallCmd.search)); // search
        headRow.add(new InlineButton(Uni.insert, CallCmd.mkDir));
        headRow.add(new InlineButton(Uni.gear, CallCmd.editMode)); // edit
        kbd.add(headRow);

        listing.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .limit(full ? listing.size() : 10)
                .forEach(f -> {
                    final List<InlineButton> row = new ArrayList<>(2);
                    row.add(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? CallCmd.cd : CallCmd.get).of(f.getId())));
                    kbd.add(row);
                });

        if (!full && listing.size() > 10)
            kbd.add(Collections.singletonList(new InlineButton("\u1801 (+" + (listing.size() - 10) + ")", CallCmd.fullLs.of(current.getId()))));

        return new InlineKeyboard(kbd);
    }

    private void doLs(final long id, final User user, final boolean full) {
        final TFile file = fsService.get(id, user);

        sendScreen(new TextRef(user.getPwd(), user.getId()).withKeyboard(makeLsScreen(file, fsService.list(id, user), full)), user);
    }
}
