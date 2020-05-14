package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.Request;
import model.telegram.api.InlineButton;
import model.telegram.api.InlineKeyboard;
import model.telegram.api.TextRef;
import model.telegram.api.UpdateRef;
import play.Logger;
import utils.CallCmd;
import utils.TextUtils;
import utils.UOpts;
import utils.Uni;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static utils.CallCmd.pageUp;
import static utils.TextUtils.*;

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
            logger.debug("Request:\n" + request);
            if (request.id > 0)
                tgApi.deleteMessage(request.id, user.getId());

            if (user.getLastDialogId() > 0) {
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());
                user.setLastDialogId(0);
                updateOpts.set(true);
            }

            if (request.file != null) {
                fsService.upload(new TFile(request.file), user);
                doLs(user.getDirId(), user);
                return;
            }

            if (UOpts.WaitFolderName.is(user)) {
                UOpts.WaitFolderName.clear(user);
                updateOpts.set(true);

                if (!isEmpty(request.text)) {
                    if (fsService.findHere(request.text, user) == null) {
                        fsService.mkdir(request.text, user.getDirId(), user.getId());
                        doLs(user.getDirId(), user);
                    } else
                        tgApi.sendPlainText("cannot create directory ‘" + request.text + "’: File exists", user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            }

            if (request.isCallback() && request.callbackCmd != null) {
                String callAnswer = "";

                switch (request.callbackCmd) {
                    case mkDir:
                        UOpts.WaitFolderName.set(user);
                        updateOpts.set(true);

                        tgApi.ask("Type new folder name:", user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });
                        break;
                    case ls:
                        doLs(request.callbackId, user);
                        break;
                    case cd:
                        final TFile dir = fsService.get(request.callbackId, user);
                        if (dir != null) {
                            callAnswer = "cd " + dir.getPath();

                            user.setDirId(request.callbackId);
                            user.setPwd(dir.getPath());
                            userService.updatePwd(user);
                            doLs(request.callbackId, user);
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
                        break;
                    case pageUp:
                    case pageDown:
                        user.setOffset(Math.max(0, user.getOffset() + (request.callbackCmd == pageUp ? 10 : -10)));
                        callAnswer = "page #" + ((user.getOffset() / 10) + 1);
                        doLs(user.getDirId(), user);
                        userService.updateOffset(user);
                        break;
                    case normalMode:
                        UOpts.GearMode.clear(user);
                        user.setOffset(0);
                        updateOpts.set(true);
                        doLs(user.getDirId(), user);
                        break;
                    case editMode:
                        UOpts.GearMode.set(user);
                        user.setSelection(",");
                        user.setOffset(0);
                        updateOpts.set(true);
                        doLs(user.getDirId(), user);
                        break;
                    case rm:
                        if (UOpts.GearMode.is(user)) {
                            final Set<Long> ids = Arrays.stream(user.getSelection().split(",")).map(TextUtils::getLong).filter(l -> l > 0).collect(Collectors.toSet());

                            fsService.rm(ids, user);
                            user.setSelection(",");
                            callAnswer = ids.size() + " entry(s) deleted";
                        } else {
                            fsService.rm(request.callbackId, user);
                            callAnswer = "Entry deleted";
                        }
                        doLs(user.getDirId(), user);
                        break;
                    case mv:
                        callAnswer = "Choose destination";
                        UOpts.MovingFile.set(user);
                        doLs(request.callbackId, user);
                        break;
                    case select:
                        if (user.getSelection().contains("," + request.callbackId + ",")) {
                            user.setSelection(user.getSelection().replace("," + request.callbackId + ",", ","));
                            tgApi.sendCallbackAnswer("deselected", request.callbackReplyId, false, 0);
                        } else {
                            user.setSelection(user.getSelection() + request.callbackId + ",");
                            tgApi.sendCallbackAnswer("selected", request.callbackReplyId, false, 0);
                        }
                        doGearLs(user.getDirId(), user);
                        updateOpts.set(true);
                        break;
                }

                tgApi.sendCallbackAnswer(callAnswer, request.callbackReplyId);
            } else
                doLs(user.getDirId(), user);
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

    private void doGearLs(final long id, final User user) {
        final TFile current = fsService.get(id, user);
        final TextRef ref = new TextRef(user.getPwd(), user.getId());
        final List<List<InlineButton>> kbd = new ArrayList<>();
        final List<InlineButton> headRow = new ArrayList<>();
        headRow.add(new InlineButton(Uni.home, CallCmd.cd.of(1L)));
        if (current.getParentId() > 1) headRow.add(new InlineButton(Uni.leftArrow, CallCmd.cd.of(current.getParentId())));
        headRow.add(new InlineButton(Uni.search, CallCmd.search));
        headRow.add(new InlineButton(Uni.back, CallCmd.normalMode));
        kbd.add(headRow);

        ref.setText(escapeMd(user.getPwd()) + "\n\n_" + escapeMd("Select entries to move or delete. Hit '" + Uni.back + "' to return to normal mode.") + "_");
        ref.setMd2();

        final List<TFile> entries = fsService.list(id, user);
        if (isEmpty(user.getSelection()))
            user.setSelection(",");

        entries.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .skip(user.getOffset())
                .limit(10)
                .forEach(f -> {
                    final List<InlineButton> row = new ArrayList<>(2);
                    row.add(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? CallCmd.cd : CallCmd.get).of(f.getId())));
                    row.add(new InlineButton(user.getSelection().contains("," + f.getId() + ",") ? Uni.checked : Uni.unchecked, CallCmd.select.of(f.getId())));
                    kbd.add(row);
                });

        final List<InlineButton> pageRow = new ArrayList<>();

        if (!entries.isEmpty()) {
            if (user.getOffset() > 0)
                pageRow.add(new InlineButton(Uni.leftArrow, CallCmd.pageDown));
            if (user.getOffset() + 10 < entries.size())
                pageRow.add(new InlineButton(Uni.rightArrow, pageUp));
        }

        if (user.getSelection().length() > 2) {
            final long size = Arrays.stream(user.getSelection().split(",")).filter(s -> getLong(s) > 0).count();
            pageRow.add(new InlineButton(Uni.move + " ("+size+")", CallCmd.mv));
            pageRow.add(new InlineButton(Uni.drop + " ("+size+")", CallCmd.rm));
        }

        if (!pageRow.isEmpty())
            kbd.add(pageRow);

        ref.setReplyMarkup(new InlineKeyboard(kbd));

        sendScreen(ref, user);

    }

    private void doMoveLs(final long id, final User user) {
        final TFile movedEntry = fsService.get(id, user);

    }

    private void doLs(final long id, final User user) {
        if (UOpts.GearMode.is(user)) {
            doGearLs(id, user);
            return;
        } else if (UOpts.MovingFile.is(user)) {
            doMoveLs(id, user);
            return;
        }

         TFile current = fsService.get(id, user);
        final TextRef ref = new TextRef(user.getPwd(), user.getId());
        final List<List<InlineButton>> kbd = new ArrayList<>();
        final List<InlineButton> headRow = new ArrayList<>();
        headRow.add(new InlineButton(Uni.home, CallCmd.cd.of(1L)));
        if (current.getParentId() > 1) headRow.add(new InlineButton(Uni.leftArrow, CallCmd.cd.of(current.getParentId())));
        headRow.add(new InlineButton(Uni.search, CallCmd.search)); // search
        headRow.add(new InlineButton(Uni.insert, CallCmd.mkDir));
        headRow.add(new InlineButton(Uni.gear, CallCmd.editMode)); // edit
        kbd.add(headRow);

        final List<TFile> entries = fsService.list(id, user);

        if (entries.isEmpty()) {
            ref.setText(escapeMd(user.getPwd()) + "\n\n_" + escapeMd("No content here yet. Send me some files.") + "_");
            ref.setMd2();
        } else {
            if (entries.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
                final StringBuilder labels = new StringBuilder(escapeMd(user.getPwd()));
                labels.append("\n\n");

                entries.stream().filter(e -> e.getType() == ContentType.LABEL)
                        .forEach(e -> labels.append('`').append(escapeMd(e.getName())).append("`\n"));

                ref.setText(labels.toString());
                ref.setMd2();
            }

            final long count = entries.stream().filter(e -> e.getType() != ContentType.LABEL).count();

            if (count > 0) {
                entries.stream().filter(e -> e.getType() != ContentType.LABEL)
                        .sorted((o1, o2) -> {
                            final int res = Boolean.compare(o2.isDir(), o1.isDir());
                            return res != 0 ? res : o1.getName().compareTo(o2.getName());
                        })
                        .skip(user.getOffset())
                        .limit(10)
                        .forEach(f -> {
                            final List<InlineButton> row = new ArrayList<>(2);
                            row.add(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? CallCmd.cd : CallCmd.get).of(f.getId())));
                            kbd.add(row);
                        });
                if (count > 10) {
                    final List<InlineButton> pageRow = new ArrayList<>();

                    if (user.getOffset() > 0)
                        pageRow.add(new InlineButton(Uni.leftArrow, CallCmd.pageDown));
                    if (user.getOffset() + 10 < count)
                        pageRow.add(new InlineButton(Uni.rightArrow, pageUp));

                    kbd.add(pageRow);
                }
            }
        }

        ref.setReplyMarkup(new InlineKeyboard(kbd));

        sendScreen(ref, user);
    }
}
