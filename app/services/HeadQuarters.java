package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.TeleFile;
import model.telegram.commands.*;
import play.Logger;
import utils.LangMap;
import utils.UOpts;
import utils.UserMode;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static utils.LangMap.v;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class HeadQuarters {
    private static final Logger.ALogger logger = Logger.of(HeadQuarters.class);

    @Inject
    private TgApi tgApi;

    @Inject
    private UserService userService;

    @Inject
    private FsService fsService;

    @Inject
    private GUI gui;

    public void accept(final TgCommand command) {
        boolean refreshView = true;
        CallbackAnswer callbackAnswer = command instanceof OfCallback ? new CallbackAnswer(command.getCallbackId()) : null;

        try {
            UOpts.clearWait(command);

            if (command.getMsgId() > 0)
                tgApi.deleteMessage(command.getMsgId(), command.getId());

            if (command.getLastDialogId() > 0)
                tgApi.deleteMessage(command.getLastDialogId(), command.getId(), aVoid -> {
                    command.setLastDialogId(0);
                    userService.updateOpts(command);
                });


            if (command instanceof RequestUserInput) {
                refreshView = false;

                tgApi.ask(makePrompt((RequestUserInput) command), command.getId(), dialogId -> {
                    command.setLastDialogId(dialogId);
                    userService.updateOpts(command);
                });
            } else if (command instanceof GotUserInput) {
                final String input = ((GotUserInput) command).getInput();

                if (command instanceof CreateDir) {
                    if (fsService.findHere(input, command) == null) {
                        fsService.mkdir(input, command.getDirId(), command.getId());
                    } else {
                        tgApi.sendPlainText(v(LangMap.Names.CANT_MKDIR, command, input), command.getId(), dialogId -> {
                            command.setLastDialogId(dialogId);
                            userService.updateOpts(command);
                        });

                        refreshView = false;
                    }
                } else if (command instanceof CreateLabel) {
                    if (fsService.findHere(input, command) == null) {
                        fsService.upload(TFile.label(input, command.getDirId()), command);
                    } else {
                        tgApi.sendPlainText(v(LangMap.Names.CANT_MKLBL, command, input), command.getId(), dialogId -> {
                            command.setLastDialogId(dialogId);
                            userService.updateOpts(command);
                        });

                        refreshView = false;
                    }
                } else if (command instanceof RenameEntry) {
                    if (fsService.findHere(input, command) == null) {
                        final TFile file = fsService.getSelectionSingle(command);
                        if (file.getName().equals(input))
                            refreshView = false;
                        else {
                            file.setName(input);
                            file.setSelected(false);
                            fsService.updateMeta(file, command);
                        }
                    } else {
                        tgApi.sendPlainText(v(LangMap.Names.CANT_RN_TO, command, input), command.getId(), dialogId -> {
                            command.setLastDialogId(dialogId);
                            userService.updateOpts(command);
                        });

                        refreshView = false;
                    }
                } else if (command instanceof Search) {
                    final int found = fsService.findChildsByName(input.toLowerCase(), command);
                    if (found > 0) {
                        command.setMode(UserMode.Searching);
                        command.setOffset(0);
                        command.setLastSearch(input);
                    } else {
                        tgApi.sendPlainText(v(LangMap.Names.NO_RESULTS, command, input), command.getId(), dialogId -> {
                            command.setLastDialogId(dialogId);
                            userService.updateOpts(command);
                        });

                        refreshView = false;
                    }
                }
            } else if (command instanceof SingleEntryOp) {
                final TFile entry = fsService.get(((SingleEntryOp) command).getEntryId(), command);

                if (entry == null || entry.getType() == ContentType.LABEL || entry.getType() == ContentType.CONTACT) {
                    tgApi.sendPlainText(v(LangMap.Names.NO_RESULTS, command, ((SingleEntryOp) command).getEntryId()), command.getId(), dialogId -> {
                        command.setLastDialogId(dialogId);
                        userService.updateOpts(command);
                    });

                    refreshView = false;
                } else {
                    if (command instanceof MoveEntry) {
                        command.setMode(UserMode.Moving);
                        command.setOffset(0);
                        fsService.newSelection(Collections.singleton(entry.getId()), command);
                    } else if (command instanceof OpenEntry) {
                        if (entry.isDir()) {
                            if (((OpenEntry) command).getUserMode() == UserMode.Searching)
                                command.setMode(UserMode.Normal);
                            ((OpenEntry) command).setDirId(entry.getId());
                            ((OpenEntry) command).setPwd(entry.getPath());
                            command.setOffset(0);
                            callbackAnswer.message = v(LangMap.Names.CD, command, entry.getPath());
                        } else {
                            gui.makeFileDialog(entry, command.getId(), dialogId -> {
                                command.setLastDialogId(dialogId);
                                userService.updateOpts(command);
                            });

                            refreshView = false;
                        }
                    } else if (command instanceof DropEntry) {
                        command.setOffset(0);
                        callbackAnswer.message = v(LangMap.Names.DELETED, command);
                        fsService.rm(entry.getId(), command);
                    } else if (command instanceof InverseSelection) {
                        entry.setSelected(!entry.isSelected());
                        fsService.updateMeta(entry, command);
                        callbackAnswer.message = v(entry.isSelected() ? LangMap.Names.SELECTED : LangMap.Names.DESELECTED, command);
                    }
                }
            } else if (command instanceof SelectionOp) {
                if (command instanceof MoveSelection) {
                    command.setOffset(0);
                    command.setMode(UserMode.Moving);
                } else if (command instanceof DropSelection) {
                    command.setOffset(0);
                    callbackAnswer.message = v(LangMap.Names.DELETED_MANY, command, fsService.rmSelected(command));
                } else if (command instanceof SelectAll) {
                    final List<TFile> scope = fsService.list(command);

                    final boolean setSelected = scope.stream().anyMatch(e -> !e.isSelected());
                    if (setSelected) {
                        fsService.setSelected(scope.stream().map(TFile::getId).collect(Collectors.toSet()), command);
                        callbackAnswer.message = v(LangMap.Names.SELECTED, command);
                    } else {
                        fsService.setDeselected(scope.stream().map(TFile::getId).collect(Collectors.toSet()), command);
                        callbackAnswer.message = v(LangMap.Names.DESELECTED, command);
                    }
                }
            } else if (command instanceof CreateFile) {
                fsService.upload(new TFile((TeleFile) command), command);
            } else if (command instanceof EditMode) {
                callbackAnswer.message = v(LangMap.Names.EDIT_MODE, command);
                command.setMode(UserMode.Gear);
                command.setOffset(0);
                fsService.newSelection(null, command);
            } else if (command instanceof SearchEditMode) {
                callbackAnswer.message = v(LangMap.Names.EDIT_MODE, command);
                command.setMode(UserMode.GearSearch);
                fsService.newSelection(null, command);
            } else if (command instanceof ExitMode) {
                if (((ExitMode) command).getUserMode() == UserMode.GearSearch) {
                    command.setMode(UserMode.Searching);
                } else {
                    callbackAnswer.message = v(LangMap.Names.NORMAL_MODE, command);
                    command.setOffset(0);
                    command.setMode(UserMode.Normal);
                }
            } else if (command instanceof Paging) {
                command.setOffset(Math.max(0, command.getOffset() + (command instanceof Forward ? 10 : -10)));
                callbackAnswer.message = v(LangMap.Names.PAGE, command, (command.getOffset() / 10) + 1);
            } else if (command instanceof MoveDestination) {
                command.setMode(UserMode.Normal);
                command.setOffset(0);

                final List<TFile> selection = fsService.getSelection(command);
                final Set<Long> predictors = fsService.getPredictors(command.getDirId(), command).stream().map(TFile::getId).collect(Collectors.toSet());
                final AtomicInteger counter = new AtomicInteger(0);
                selection.stream().filter(f -> !f.isDir() || !predictors.contains(f.getId())).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(command.getDirId()));
                fsService.updateMetas(selection, command);
                callbackAnswer.message = v(LangMap.Names.MOVED, command, counter.get());
            } else if (command instanceof SimpleCancel) {
                refreshView = false; // закрытие файл-бокса или подобное событие
            }

            if (refreshView)
                gui.makeMainView((User) command, msgId -> {
                    if (msgId == 0 || msgId != command.getLastMessageId()) {
                        command.setLastMessageId(msgId);
                        userService.updateOpts(command);
                    }
                });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            userService.updateOpts(command);
            if (callbackAnswer != null)
                tgApi.sendCallbackAnswer(callbackAnswer.message, callbackAnswer.forId, callbackAnswer.alert, callbackAnswer.cacheSeconds);
        }
    }

    private String makePrompt(final RequestUserInput command) {
        if (command instanceof SearchReq)
            return v(LangMap.Names.TYPE_QUERY, (User) command);

        if (command instanceof RenameEntryReq)
            return v(LangMap.Names.TYPE_RENAME, (User) command, fsService.get(((RenameEntryReq) command).entryId, (User) command).getName());

        if (command instanceof CreateLabelReq)
            return v(LangMap.Names.TYPE_LABEL, (User) command);

        // CreateDirReq
        return v(LangMap.Names.TYPE_FOLDER, (User) command);
    }

    private static class CallbackAnswer {
        long forId;
        String message = "";
        boolean alert = false;
        int cacheSeconds = 0;

        public CallbackAnswer(final long forId) {
            this.forId = forId;
        }
    }
}
