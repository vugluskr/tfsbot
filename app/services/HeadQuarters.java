package services;

import com.typesafe.config.Config;
import model.Callback;
import model.Share;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import play.Logger;
import utils.*;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static utils.LangMap.v;
import static utils.Strings.Callback.drop;
import static utils.Strings.Callback.move;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class HeadQuarters {
    private static final Logger.ALogger logger = Logger.of(HeadQuarters.class);
    private static final Comparator<TFile> sorter = (o1, o2) -> {
        final int res = Boolean.compare(o2.isDir(), o1.isDir());
        return res != 0 ? res : o1.getName().compareTo(o2.getName());
    };

    @Inject
    private Config config;

    @Inject
    private TfsService fsService;

    @Inject
    private UserService userService;

    @Inject
    private GUI gui;

    public void callback(final String callbackData, final User user) {
        try {
            boolean shouldUpdate = false, fallToView = true;

            final int idx = callbackData.indexOf(':');

            if (idx < 1)
                logger.debug("Неизвестный науке коллбек: " + callbackData);
            else {
                final Callback c = new Callback(Strings.Callback.ofString(callbackData), idx < callbackData.length() - 1 ? getInt(callbackData.substring(idx + 1)) : -1);
                final TFile meantEntry = c.idx == -1 || user.isSharing() ? null : scope(user).stream().filter(scopeFilter(user)).sorted(sorter).toArray(TFile[]::new)[c.idx];
                final Share meantShare = c.idx == -1 || !user.isSharing() ? null : scopeShares(user).stream().filter(s -> !s.isGlobal()).sorted(Comparator.comparing(Share::getName)).toArray(Share[]::new)[c.idx];
                final TFile selected = meantEntry == null && !user.selection.isEmpty() ? fsService.get(user.selection.first(), user) : null;
                final TFile current = fsService.get(user.getCurrentDirId(), user);

                switch (c.type) {
                    case cancel:
                        if (user.isWaitInput())
                            user.cancelWaiting();
                        else if (user.isGearing())
                            user.unsetGearing();
                        else if (user.isSearching())
                            user.unsetSearching();
                        else if (user.isFileViewing())
                            user.unsetFileViewing();
                        else if (user.isSharing())
                            user.unsetSharing();
                        else if (user.isMoving())
                            user.unsetMoving();

                        shouldUpdate = true;
                        break;
                    case changeRo:
                        if (meantShare != null)
                            fsService.changeShareRo(meantShare.getId(), user);
                        break;
                    case checkAll:
                        userService.bulkSelection(scope(user), user);
                        break;
                    case drop:
                        if (user.isSharing()) {
                            if (meantShare != null)
                                fsService.dropShare(meantShare.getId(), user);

                            if (selected != null && fsService.noSharesExist(user.selection.first(), user)) {
                                selected.setUnshared();
                                fsService.updateMeta(selected, user);
                            }
                        } else {
                            fsService.rmSelected(user);
                            userService.resetSelection(user);
                        }
                        break;
                    case forward:
                        userService.pageUp(user);
                        break;
                    case gear:
                        user.setGearing();
                        userService.update(user);
                        break;
                    case goUp:
                        if (!user.isOnTop())
                            userService.dirChange(fsService.get(user.getCurrentDirId(), user).getParentId(), user);
                        break;
                    case inversCheck:
                        if (meantEntry != null)
                            userService.entryInversSelection(meantEntry, user);
                        break;
                    case mkDir:
                        gui.dialog(LangMap.Value.TYPE_FOLDER, user);
                        user.setWaitDirInput();
                        shouldUpdate = true;
                        fallToView = false;
                        break;
                    case mkGrant:
                        if (selected != null) {
                            gui.dialog(selected.isDir() ? LangMap.Value.SEND_CONTACT_DIR : LangMap.Value.SEND_CONTACT_FILE, user, selected.getPath());
                            user.setWaitFileGranting();
                            shouldUpdate = true;
                            fallToView = false;
                        }
                        break;
                    case mkLabel:
                        gui.dialog(LangMap.Value.TYPE_LABEL, user);
                        user.setWaitLabelInput();
                        shouldUpdate = true;
                        fallToView = false;
                        break;
                    case mkLink:
                        final TFile entry = fsService.get(user.selection.first(), user);
                        final Share share = fsService.listShares(entry.getId(), user).stream().filter(Share::isGlobal).findAny().orElse(null);

                        gui.yesNoPrompt(share == null
                                        ? (entry.isDir() ? LangMap.Value.CREATE_PUBLINK_DIR : LangMap.Value.CREATE_PUBLINK_FILE)
                                        : (entry.isDir() ? LangMap.Value.DROP_PUBLINK_DIR : LangMap.Value.DROP_PUBLINK_FILE),
                                user, TextUtils.escapeMd(entry.getPath()));

                        fallToView = false;
                        break;
                    case move:
                        user.setMoving();
                        shouldUpdate = true;
                        break;
                    case ok:
                        if (selected != null) {
                            if (fsService.isGlobalShareMissed(selected.getId(), user)) {
                                fsService.makeShare(selected.getName(), user, selected.getId(), 0, null);

                                if (!selected.isShared()) {
                                    selected.setShared();
                                    fsService.updateMeta(selected, user);
                                }
                            } else {
                                fsService.dropGlobalShareByEntry(selected.getId(), user);
                                if (fsService.noSharesExist(selected.getId(), user)) {
                                    selected.setUnshared();
                                    fsService.updateMeta(selected, user);
                                }
                            }
                        }
                        break;
                    case open:
                        if (meantEntry != null && meantEntry.isDir())
                            userService.dirChange(meantEntry.getId(), user);
                        else {
                            gui.makeFileDialog(meantEntry, userService.entrySelectedSolo(meantEntry, user));
                            user.setFileViewing();
                            shouldUpdate = true;
                            fallToView = false;
                        }
                        break;
                    case put:
                        if (!current.isRw()) {
                            gui.dialog(LangMap.Value.NOT_ALLOWED, user);
                            fallToView = false;
                        } else {
                            final Set<UUID> predictors = fsService.getPredictors(user).stream().map(TFile::getId).collect(Collectors.toSet());
                            final List<TFile> selection = fsService.getSelection(user);
                            final AtomicInteger counter = new AtomicInteger(0);
                            selection.stream()
                                    .filter(f -> f.isRw() && (!f.isDir() || !predictors.contains(f.getId()))).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(current.getId()));

                            fsService.bulkUpdate(selection);
                            user.unsetMoving();
                            shouldUpdate = true;
                        }
                        break;
                    case rename:
                        gui.dialog(LangMap.Value.TYPE_RENAME, user, fsService.get(user.selection.first(), user).getName());
                        user.setWaitRenameInput();
                        shouldUpdate = true;
                        fallToView = false;
                        break;
                    case rewind:
                        userService.pageDown(user);
                        break;
                    case search:
                        gui.dialog(LangMap.Value.TYPE_QUERY, user);
                        user.setWaitSearchInput();
                        shouldUpdate = true;
                        fallToView = false;
                        break;
                    case share:
                        user.setSharing();
                        shouldUpdate = true;
                        break;
                }
            }

            if (shouldUpdate)
                userService.update(user);

            if (fallToView)
                doView(user);
            else
                logger.debug(user.toString());
        } catch (final Exception e) {
            logger.error("Callback fuckup: " + e.getMessage(), e);
        }
    }

    public void text(final String input, final User user) {
        try {
            boolean fallToView = true;

            if (input.equalsIgnoreCase("/start") || input.equalsIgnoreCase("/reset")) {
                user.setOptions(0);
                user.setCurrentDirId(user.getRootId());
            } else if (notNull(input).startsWith("/start shared-")) {
                final String id = notNull(input).substring(14);

                final Share share;
                if (!id.isEmpty() && (share = fsService.getPublicShare(id)) != null && share.getOwner() != user.getId()) {
                    user.resetState();
                    user.resetInputWait();

                    final TFile dir = fsService.applyShareByLink(share, user);

                    if (dir != null)
                        userService.dirChange(dir.getId(), user);
                }
            } else {
                if (user.isWaitDirInput()) {
                    if (!fsService.get(user.getCurrentDirId(), user).isRw()) {
                        gui.dialog(LangMap.Value.NOT_ALLOWED, user, input);
                        fallToView = false;
                    } else if (fsService.entryExist(input, user)) {
                        gui.dialog(LangMap.Value.CANT_MKDIR, user, input);
                        fallToView = false;
                    } else
                        fsService.mk(TFileFactory.dir(input, user.getCurrentDirId(), user.getId()));
                } else if (user.isWaitLabelInput()) {
                    if (!fsService.get(user.getCurrentDirId(), user).isRw()) {
                        gui.dialog(LangMap.Value.NOT_ALLOWED, user, input);
                        fallToView = false;
                    } else if (fsService.entryExist(input, user)) {
                        gui.dialog(LangMap.Value.CANT_MKLBL, user, input);
                        fallToView = false;
                    } else
                        fsService.mk(TFileFactory.label(input, user.getCurrentDirId(), user.getId()));
                } else if (user.isWaitRenameInput()) {
                    if (fsService.entryExist(input, user)) {
                        gui.dialog(LangMap.Value.CANT_RN_TO, user, input);
                        fallToView = false;
                    }

                    final TFile entry;
                    if (!user.selection.isEmpty() && !(entry = fsService.get(user.selection.first(), user)).getName().equals(input)) {
                        if (!entry.isRw()) {
                            gui.dialog(LangMap.Value.NOT_ALLOWED_THIS, user, input);
                            fallToView = false;
                        }

                        entry.setName(input);
                        entry.setPath(Paths.get(entry.getPath()).getParent().resolve(input).toString());
                        fsService.updateMeta(entry, user);
                        userService.resetSelection(user);
                    }
                } else
                    userService.searched(notNull(input), user);

                user.resetInputWait();
            }

            userService.update(user);

            if (fallToView)
                doView(user);
            else
                logger.debug(user.toString());
        } catch (final Exception e) {
            logger.error("Input fuckup: " + e.getMessage(), e);
        }
    }

    public void file(final TFile file, final User user) {
        try {
            boolean fallToView = true;

            if (user.isWaitFileGranting() && file.getType() == ContentType.CONTACT) {
                final User contact = new User();
                contact.setId(file.getOwner());
                contact.name = file.getName();
                contact.setLang(user.getLang());

                final User target = userService.resolveUser(contact);
                final TFile entry = fsService.get(user.selection.first(), user);

                if (fsService.shareExist(entry.getId(), target.getId(), user)) {
                    gui.dialog(LangMap.Value.CANT_GRANT, user, file.getName());
                    fallToView = false;
                } else {
                    fsService.makeShare(file.getName(), user, entry.getId(), target.getId(), notNull(target.getLang(), "en"));
                    if (!entry.isShared()) {
                        entry.setShared();
                        fsService.updateMeta(entry, user);
                    }
                }
            } else {
                file.setOwner(user.getId());
                file.setParentId(user.getCurrentDirId());

                fsService.mk(file);
            }

            user.unsetWaitFileGranting();
            user.resetState();
            userService.update(user);

            if (fallToView)
                doView(user);
            else
                logger.debug(user.toString());
        } catch (final Exception e) {
            logger.error("File fuckup: " + e.getMessage(), e);
        }
    }

    private Predicate<TFile> scopeFilter(final User user) {
        return
                (user.isSearching() && !user.isGearing()) || user.isJustWatching()
                        ? file -> file.getType() != ContentType.LABEL
                        : file -> true;
    }

    private List<TFile> scope(final User user) {
        return user.isMoving()
                ? fsService.listFolders(user)
                : user.isSearching()
                ? fsService.search(user.getQuery(), user)
                : fsService.list(user);
    }

    private List<Share> scopeShares(final User user) {
        return fsService.listShares(user.selection.first(), user);
    }

    private void doView(final User user) {
        if (user.isSharing()) {
            doShareView(user);
            return;
        }

        final TFile current = fsService.get(user.getCurrentDirId(), user);
        final List<TFile> scope = scope(user);

        final FlowBox box = new FlowBox().md2();

        stateBody(current, scope, box, user);
        stateButtons(current, scope, box, user);

        final AtomicInteger indexer = new AtomicInteger(0);

        scope.stream().filter(scopeFilter(user))
                .sorted(sorter)
                .skip(user.getViewOffset())
                .limit(10)
                .forEach(f -> stateItemBox(f, current, box, user, indexer));

        gui.sendBox(box.setListing(user.getViewOffset() > 0, user.getViewOffset() + 10 < scope.stream().filter(scopeFilter(user)).count()), user);

        logger.debug(user.toString());
    }

    private void stateButtons(final TFile current, final List<TFile> scope, final FlowBox box, final User user) {
        if (user.isGearing()) {
            if (!isEmpty(user.selection)) {
                if (user.selection.size() == 1)
                    scope.stream().filter(e -> e.getId().equals(user.selection.first())).findAny().ifPresent(file -> {
                        if (file.isSharable()) box.button(GUI.Buttons.shareButton);
                        box.button(GUI.Buttons.renameButton);
                    });

                final long count = user.selection.isEmpty() ? 0 : scope.stream().filter(f -> f.isRw() && user.selection.contains(f.getId())).count();

                if (count > 0) {
                    box
                            .button(new GUI.Button(Strings.Uni.move + "(" + count + ")", move))
                            .button(new GUI.Button(Strings.Uni.drop + "(" + user.selection.size() + ")", drop));
                }

                if (count > 1)
                    box.button(GUI.Buttons.checkAllButton);
            }

            box.button(GUI.Buttons.cancelButton);
        } else if (user.isMoving()) {
            if (!user.isOnTop())
                box.button(GUI.Buttons.goUpButton);

            box.button(GUI.Buttons.putButton)
                    .button(GUI.Buttons.cancelButton);
        } else if (user.isSearching()) {
            box.button(GUI.Buttons.searchButton);
            if (!scope.isEmpty())
                box.button(GUI.Buttons.gearButton);
            box.button(GUI.Buttons.cancelButton);
        } else {
            if (!user.isOnTop())
                box.button(GUI.Buttons.goUpButton);

            if (current.isRw())
                box.button(GUI.Buttons.mkLabelButton)
                        .button(GUI.Buttons.mkDirButton);

            box.button(GUI.Buttons.searchButton);
//            if (current.isRw()) box.button(GUI.Buttons.gearButton);
            /*if (file.isSharable()) */box.button(GUI.Buttons.shareButton);
            box.button(GUI.Buttons.renameButton);
            box.button(GUI.Buttons.dropButton);
        }
    }

    private void stateBody(final TFile current, final List<TFile> scope, final FlowBox box, final User user) {
        box.body(notNull(escapeMd(current.getPath()), "/") + "\n");

        if (user.isGearing()) {
            if (user.isSearching())
                box.body("\n_" + escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), scope.size())) + "_");
            else if (scope.isEmpty())
                box.body("\n_" + escapeMd(v(LangMap.Value.NO_CONTENT, user)) + "_");
        } else if (user.isSearching()) {
            box.body("\n" + escapeMd(v(scope.isEmpty() ? LangMap.Value.NO_RESULTS : LangMap.Value.SEARCHED, user, user.getQuery(), scope.size())));

            if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
                box.body("\n");
                scope.stream().filter(e -> e.getType() == ContentType.LABEL).forEach(e -> box.body('`' + escapeMd(e.getName()) + "`\n\n"));
            }
        } else {
            if (scope.isEmpty())
                box.body("\n_" + escapeMd(v(LangMap.Value.NO_CONTENT, user)) + "_");

            if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
                box.body("\n");
                scope.stream().filter(e -> e.getType() == ContentType.LABEL).forEach(e -> box.body('`' + escapeMd(e.getName()) + "`\n\n"));
            }
        }
    }

    private void stateItemBox(final TFile f, final TFile currentDir, final FlowBox box, final User user, final AtomicInteger indexer) {
        if (user.isGearing()) {
            box.row().button(new GUI.Button(
                    (f.isShared() ? Strings.Uni.share + " " : "")
                            + (f.isDir() ? Strings.Uni.folder + " " : "")
                            + (user.isSearching() ? f.getPath().substring(currentDir.getPath().length()) : f.getName())
                            + (user.selection.contains(f.getId()) ? " " + Strings.Uni.checked : ""),
                    Strings.Callback.inversCheck.toString() + indexer.getAndIncrement()));
        } else if (user.isSearching()) {
            box.row().button(new GUI.Button((f.isDir() ? Strings.Uni.folder + " " : "") + f.getPath().substring(currentDir.getPath().length()),
                    Strings.Callback.open.toString() + indexer.getAndIncrement()));
        } else
            box.row().button(new GUI.Button((f.isDir() ? Strings.Uni.folder + " " : "") + f.getName(), Strings.Callback.open.toString() + indexer.getAndIncrement()));
    }

    private void doShareView(final User user) {
        final TFile dir = fsService.get(user.selection.first(), user);
        final List<Share> scope = dir.isShared() ? fsService.listShares(dir.getId(), user) : Collections.emptyList();
        final Share glob = scope.stream().filter(s -> s.getSharedTo() == 0).findAny().orElse(null);
        final long countPers = scope.stream().filter(s -> s.getSharedTo() > 0).count();

        final FlowBox box = new FlowBox()
                .md2()
                .body("*" + escapeMd(dir.getPath()) + "*\n\n")
                .body(Strings.Uni.Link + ": _" +
                        escapeMd((glob != null
                                ? "https://t.me/" + config.getString("service.bot.nick") + "?start=shared-" + glob.getId()
                                : v(LangMap.Value.NO_GLOBAL_LINK, user)
                        )) + "_\n");

        if (countPers <= 0)
            box.body(Strings.Uni.People + ": _" + escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user)) + "_");

        box.button(GUI.Buttons.mkLinkButton)
                .button(GUI.Buttons.mkGrantButton)
                .button(GUI.Buttons.cancelButton);

        final AtomicInteger counter = new AtomicInteger(0);

        scope.stream()
                .filter(s -> !s.isGlobal())
                .sorted(Comparator.comparing(Share::getName))
                .forEach(s -> box.row()
                        .button(new GUI.Button(v(s.isReadWrite() ? LangMap.Value.SHARE_ACCESS : LangMap.Value.SHARE_ACCESS_RO, user, s.getName()),
                                Strings.Callback.changeRo.toString() + counter.get()))
                        .button(new GUI.Button(Strings.Uni.drop, Strings.Callback.drop.toString() + counter.getAndIncrement())));

        gui.sendBox(box, user);
        logger.debug(user.toString());
    }
}
