package services;

import model.TFile;
import model.User;
import model.telegram.api.*;
import play.Logger;
import utils.TextUtils;
import utils.UOpts;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
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

    public void handle(final UpdateRef input, final User user) {
        try {
            final String callback;
            if (input.getCallback() != null && !isEmpty((callback = input.getCallback().getData()))) {
                final long id = TextUtils.getLong(callback);
                final String cmd = id > 0 ? callback.substring(0, callback.indexOf(String.valueOf(id))) : callback;

                switch (cmd) {
                    case c.mkDir:
                        UOpts.WaitFolderName.set(user);
                        userService.updateOpts(user);
                        tgApi.sendMessage(new TextRef("New folder name:", user.getId()).withForcedReply());
                        break;
                    case c.ls:
                        doLs(id, user);
                        break;
                    case c.cd:
                        final TFile file = fsService.get(id, user);
                        if (file != null) {
                            user.setDirId(id);
                            user.setPwd(file.getPath());
                            userService.updatePwd(user);
                            doLs(id, user);
                        }
                        break;
                }
            } else {
                doLs(user.getDirId(), user);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
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

    private InlineKeyboard makeLsScreen(final TFile current, final Collection<TFile> listing) {
        final List<List<InlineButton>> kbd = new ArrayList<>();
        final List<InlineButton> headRow = new ArrayList<>();
        if (current.getId() > 1) headRow.add(new InlineButton("\u23cf", c.cd + current.getParentId()));
        headRow.add(new InlineButton("\u2398", c.mkDir));
        kbd.add(headRow);

        listing.stream().sorted((o1, o2) -> {
            final int res = Boolean.compare(o2.isDir(), o1.isDir());
            return res != 0 ? res : o1.getName().compareTo(o2.getName());
        }).forEach(f -> {
            final List<InlineButton> row = new ArrayList<>(2);
//            row.add(new InlineButton("\u238a", c.rm + f.getId()));
//            row.add(new InlineButton("\u2702", c.mv + f.getId()));
//            row.add(new InlineButton("\u2380", c.mv + f.getId()));
            row.add(new InlineButton((f.isDir() ? "\uD83D\uDCC2 " : "") + f.getName(), (f.isDir() ? c.cd : c.get) + f.getId()));
            kbd.add(row);
        });

        return new InlineKeyboard(kbd);
    }

    private void doLs(final long id, final User user) {
        final TFile file = fsService.get(id, user);

        sendScreen(new TextRef(user.getPwd(), user.getId()).withKeyboard(makeLsScreen(file, fsService.list(id, user))), user);
    }

    private interface c {
        String mkDir = "mk";
        String ls = "ls_";
        String cd = "cd_";
        String rm = "rm_";
        String mv = "mv_";
        String get = "gt_";
    }
}
