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
import utils.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static utils.CallCmd.pageUp;
import static utils.LangMap.v;
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
                        tgApi.sendPlainText(v(LangMap.Names.CANT_MKDIR, user, request.text), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            } else if (UOpts.WaitFileName.is(user)) {
                UOpts.WaitFileName.clear(user);
                updateOpts.set(true);

                if (!isEmpty(request.text)) {
                    if (fsService.findHere(request.text, user) == null) {
                        final TFile file = fsService.get(getLong(user.getSelection()), user);
                        file.setName(request.text);
                        fsService.updateMeta(file, user);
                        doLs(user.getDirId(), user);
                        user.setSelection("");
                    } else
                        tgApi.sendPlainText(v(LangMap.Names.CANT_RN_TO, user, request.text), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            } else if (UOpts.WaitLabelText.is(user)) {
                UOpts.WaitLabelText.clear(user);
                updateOpts.set(true);

                if (!isEmpty(request.text)) {
                    if (fsService.findHere(request.text, user) == null) {
                        final TFile file = new TFile(ContentType.LABEL, 0, "--", "--", request.text);
                        fsService.upload(file, user);
                        doLs(user.getDirId(), user);
                    } else
                        tgApi.sendPlainText(v(LangMap.Names.CANT_MKLBL, user, request.text), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            } else if (UOpts.WaitSearchQuery.is(user)) {
                UOpts.WaitSearchQuery.clear(user);
                updateOpts.set(true);

                if (!isEmpty(request.text)) {
                    final List<TFile> results = fsService.findChildsByName(request.text.toLowerCase(), user);
                    if (!isEmpty(results)) {
                        user.setSelection(results.stream().map(f -> String.valueOf(f.getId())).collect(Collectors.joining(",")));
                        doSearchLs(request.text, results, user);
                    } else
                        tgApi.sendPlainText(v(LangMap.Names.NO_RESULTS, user, request.text), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });

                    return;
                }
            }

            if (request.isCallback() && request.callbackCmd != null) {
                String callAnswer = "";
                int answerCache = 0;
                boolean answerAlert = false;

                switch (request.callbackCmd) {
                    // todo prefs
                    // todo localization
                    case search:
                        UOpts.WaitSearchQuery.set(user);
                        updateOpts.set(true);

                        tgApi.ask(v(LangMap.Names.TYPE_QUERY, user), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });
                        break;
                    case rename:
                        UOpts.WaitFileName.set(user);
                        user.setSelection(String.valueOf(request.callbackId));
                        updateOpts.set(true);

                        tgApi.ask(v(LangMap.Names.TYPE_RENAME, user, fsService.get(request.callbackId, user).getName()), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });
                        break;
                    case mkDir:
                        UOpts.WaitFolderName.set(user);
                        updateOpts.set(true);

                        tgApi.ask(v(LangMap.Names.TYPE_FOLDER, user), user.getId(), dialogId -> {
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
                            callAnswer = v(LangMap.Names.CD, user, dir.getPath());
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
                        callAnswer = v(LangMap.Names.PAGE, user, (user.getOffset() / 10) + 1);
                        answerCache = 0;
                        doLs(user.getDirId(), user);
                        userService.updateOffset(user);
                        break;
                    case normalMode:
                        callAnswer = v(LangMap.Names.NORMAL_MODE, user);
                        UOpts.GearMode.clear(user);
                        UOpts.MovingFile.clear(user);
                        user.setOffset(0);
                        updateOpts.set(true);
                        doLs(user.getDirId(), user);
                        break;
                    case editMode:
                        callAnswer = v(LangMap.Names.EDIT_MODE, user);
                        UOpts.GearMode.set(user);
                        UOpts.MovingFile.clear(user);
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
                            callAnswer = v(LangMap.Names.DELETED_MANY, user, ids.size());
                        } else {
                            fsService.rm(request.callbackId, user);
                            callAnswer = v(LangMap.Names.DELETED, user);
                        }
                        user.setOffset(0);
                        updateOpts.set(true);
                        doLs(user.getDirId(), user);
                        break;
                    case mv:
                        callAnswer = v(LangMap.Names.MOVE_DEST, user);
                        UOpts.MovingFile.set(user);
                        if (!UOpts.GearMode.is(user))
                            user.setSelection(String.valueOf(request.callbackId));
                        updateOpts.set(true);
                        doLs(request.callbackId, user);
                        break;
                    case select:
                        if (user.getSelection().contains("," + request.callbackId + ",")) {
                            user.setSelection(user.getSelection().replace("," + request.callbackId + ",", ","));
                            callAnswer = v(LangMap.Names.DESELECTED, user);
                        } else {
                            user.setSelection(user.getSelection() + request.callbackId + ",");
                            callAnswer = v(LangMap.Names.SELECTED, user);
                        }
                        doGearLs(user.getDirId(), user);
                        updateOpts.set(true);
                        break;
                    case put:
                        final Set<Long> ids = Arrays.stream(user.getSelection().split(",")).map(TextUtils::getLong).filter(l -> l > 0).collect(Collectors.toSet());
                        UOpts.MovingFile.clear(user);
                        user.setSelection("");
                        updateOpts.set(true);

                        final List<TFile> selection = fsService.getByIds(ids, user);
                        final Set<Long> predictors = fsService.getPredictors(user.getDirId(), user).stream().map(TFile::getId).collect(Collectors.toSet());
                        final AtomicInteger counter = new AtomicInteger(0);
                        selection.stream().filter(f -> !f.isDir() || !predictors.contains(f.getId())).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(user.getDirId()));
                        fsService.updateMetas(selection, user);
                        callAnswer = v(LangMap.Names.MOVED, user, counter.get());
                        doLs(user.getDirId(), user);
                        break;
                    case label:
                        UOpts.WaitLabelText.set(user);
                        updateOpts.set(true);

                        tgApi.ask(v(LangMap.Names.TYPE_LABEL, user), user.getId(), dialogId -> {
                            user.setLastDialogId(dialogId);
                            userService.updateOpts(user);
                        });
                        break;
                }

                tgApi.sendCallbackAnswer(callAnswer, request.callbackReplyId, answerAlert, answerCache);
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
        final TextRef ref = initLsRef(current, user, false);
        ref.headRow(new InlineButton(Uni.checkAll, CallCmd.search));
        ref.headRow(new InlineButton(Uni.back, CallCmd.normalMode));

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
//                .forEach(f -> ref.row(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? CallCmd.cd : CallCmd.get).of(f.getId())),
//                        new InlineButton(user.getSelection().contains("," + f.getId() + ",") ? Uni.checked : Uni.unchecked, CallCmd.select.of(f.getId()))));
        .forEach(f -> ref.row(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName() + (user.getSelection().contains("," + f.getId() + ",") ? Uni.checked : ""),
                CallCmd.select.of(f.getId()))));

        final List<InlineButton> pageRow = new ArrayList<>();

        if (!entries.isEmpty()) {
            if (user.getOffset() > 0)
                pageRow.add(new InlineButton(Uni.leftArrow, CallCmd.pageDown));
            if (user.getOffset() + 10 < entries.size())
                pageRow.add(new InlineButton(Uni.rightArrow, pageUp));
        }

        final long size = Arrays.stream(user.getSelection().split(",")).filter(s -> getLong(s) > 0).count();
        if (size > 0) {
            if (size == 1)
                pageRow.add(new InlineButton(Uni.rename + " (" + size + ")", CallCmd.rename.of(getLong(user.getSelection()))));
            pageRow.add(new InlineButton(Uni.move + " (" + size + ")", CallCmd.mv));
            pageRow.add(new InlineButton(Uni.drop + " (" + size + ")", CallCmd.rm));
        }

        if (!pageRow.isEmpty())
            ref.row(pageRow);

        sendScreen(ref, user);

    }

    private void doMoveLs(final long id, final User user) {
        final TFile currentDir = fsService.get(user.getDirId(), user);
        final TextRef ref = initLsRef(currentDir, user, false);
//        ref.headRow(new InlineButton(Uni.search, CallCmd.search));
        ref.headRow(new InlineButton(Uni.back, UOpts.GearMode.is(user) ? CallCmd.editMode : CallCmd.normalMode));

//        final Set<Long> subjects =
//                UOpts.GearMode.is(user)
//                        ? Arrays.stream(user.getSelection().split(",")).map(TextUtils::getLong).filter(s -> s > 0).collect(Collectors.toSet())
//                        : Collections.singleton(id);

//        if (!subjects.contains(id))
        ref.row(new InlineButton(Uni.target, CallCmd.put));

        casualListing(fsService.listFolders(id, user), ref, user);

        sendScreen(ref, user);
    }

    private void doLs(final long id, final User user) {
        if (UOpts.GearMode.is(user)) {
            if (UOpts.MovingFile.is(user))
                doMoveLs(id, user);
            else
                doGearLs(id, user);
            return;
        } else if (UOpts.MovingFile.is(user)) {
            doMoveLs(id, user);
            return;
        }

        final TFile current = fsService.get(id, user);
        final TextRef ref = initLsRef(current, user, true);
        ref.headRow(new InlineButton(Uni.search, CallCmd.search)); // search
        ref.headRow(new InlineButton(Uni.insert, CallCmd.mkDir));
        ref.headRow(new InlineButton(Uni.gear, CallCmd.editMode)); // edit

        casualListing(fsService.list(id, user), ref, user);

        sendScreen(ref, user);
    }

    private void doSearchLs(final String query, final List<TFile> entries, final User user) {
        final TFile current = fsService.get(user.getDirId(), user);
        final TextRef ref = initLsRef(current, user, false);
        ref.setText(v(LangMap.Names.SEARCHED, user, query, user.getPwd()));
        ref.headRow(new InlineButton(Uni.back, CallCmd.cd.of(1)));

        String remove = current.getPath() + "/";
        entries.forEach(r -> r.setName(r.getPath().replace(remove, "")));
        casualListing(entries, ref, user);

        sendScreen(ref, user);
    }

    private void casualListing(final List<TFile> entries, final TextRef ref, final User user) {
        if (entries.isEmpty()) {
            if (!UOpts.MovingFile.is(user)) {
                ref.setText(escapeMd(user.getPwd()) + "\n\n_" + escapeMd(v(LangMap.Names.NO_CONTENT, user)) + "_");
                ref.setMd2();
            }
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
                        .forEach(f -> ref.row(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName(), (f.isDir() ? CallCmd.cd : CallCmd.get).of(f.getId()))));
                if (count > 10) {
                    final List<InlineButton> pageRow = new ArrayList<>();

                    if (user.getOffset() > 0)
                        pageRow.add(new InlineButton(Uni.leftArrow, CallCmd.pageDown));
                    if (user.getOffset() + 10 < count)
                        pageRow.add(new InlineButton(Uni.rightArrow, pageUp));

                    ref.row(pageRow);
                }
            }
        }
    }

    private TextRef initLsRef(final TFile current, final User user, final boolean withLabel) {
        final TextRef ref = new TextRef(user.getPwd(), user.getId());
//        ref.headRow(new InlineButton(Uni.home, CallCmd.cd.of(1L)));
        if (current.getParentId() > 0) ref.headRow(new InlineButton(Uni.home, CallCmd.cd.of(current.getParentId())));
        if (withLabel)
            ref.headRow(new InlineButton(Uni.label, CallCmd.label.of(current.getId())));

        return ref;
    }

    public void doPrefs(final User user) {

    }
}
