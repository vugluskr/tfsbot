package controllers;

import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.TeleFile;
import model.telegram.api.UpdateRef;
import model.telegram.commands.*;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.HeadQuarters;
import services.UserService;
import utils.TextUtils;
import utils.UOpts;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class Handler extends Controller {
    private static final Logger.ALogger logger = Logger.of(Handler.class);

    @Inject
    private HeadQuarters hq;

    @Inject
    private UserService userService;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {
            if (request.hasBody()) {
                logger.debug("INCOMING:\n" + request.body().asText());
                final UpdateRef updateRef = Json.fromJson(request.body().asJson(), UpdateRef.class);

                if (updateRef != null) {
                    final User user = userService.getUser(updateRef);

                    final long id = updateRef.getMessage() != null ? updateRef.getMessage().getMessageId() : 0;
                    final String text = updateRef.getMessage() != null ? updateRef.getMessage().getText() : null;
                    final String callback = updateRef.getCallback() != null ? updateRef.getCallback().getData() : null;
                    final long callbackId = callback == null ? 0 : updateRef.getCallback().getId();
                    final long callbackSubjectId = callback == null ? 0 : TextUtils.getLong(callback);
                    final TeleFile file = updateRef.getMessage() != null ? updateRef.getMessage().getTeleFile() : null;
                    final ContactRef sentContact = updateRef.getMessage() != null ? updateRef.getMessage().getTgUser() : null;

                    final TgCommand command;

                    if (file != null)
                        command = new CreateFile(
                                notNull(file.getFileName(), file.getType().name().toLowerCase() + "_" + file.getUniqId() + file.getType().ext),
                                file.getType().name(),
                                file.getFileId(),
                                file.getUniqId(),
                                file.getFileSize(),
                                id,
                                callbackId,
                                user
                        );
                    else if (!isEmpty(callback)) {
                        if (InverseSelection.is(callback))
                            command = new InverseSelection(callbackSubjectId, id, callbackId, user);
                        else if (CreateDirReq.is(callback))
                            command = new CreateDirReq(id, callbackId, user);
                        else if (CreateLabelReq.is(callback))
                            command = new CreateLabelReq(id, callbackId, user);
                        else if (DropEntry.is(callback))
                            command = new DropEntry(callbackSubjectId, id, callbackId, user);
                        else if (DropSelection.is(callback))
                            command = new DropSelection(id, callbackId, user);
                        else if (ExitMode.is(callback))
                            command = new ExitMode(id, callbackId, user);
                        else if (Forward.is(callback))
                            command = new Forward(id, callbackId, user);
                        else if (MoveEntry.is(callback))
                            command = new MoveEntry(callbackSubjectId, id, callbackId, user);
                        else if (MoveSelection.is(callback))
                            command = new MoveSelection(id, callbackId, user);
                        else if (OpenEntry.is(callback))
                            command = new OpenEntry(callbackSubjectId, id, callbackId, user);
                        else if (RenameEntryReq.is(callback))
                            command = new RenameEntryReq(callbackSubjectId, id, callbackId, user);
                        else if (Rewind.is(callback))
                            command = new Rewind(id, callbackId, user);
                        else if (SearchReq.is(callback))
                            command = new SearchReq(id, callbackId, user);
                        else if (SelectAll.is(callback))
                            command = new SelectAll(id, callbackId, user);
                        else if (EditMode.is(callback))
                            command = new EditMode(id, callbackId, user);
                        else if (SearchEditMode.is(callback))
                            command = new SearchEditMode(id, callbackId, user);
                        else if (MoveDestination.is(callback))
                            command = new MoveDestination(id, callbackId, user);
                        else if (SimpleCancel.is(callback))
                            command = new SimpleCancel(id, callbackId, user);
                        else
                            command = null;
                    } else if (!isEmpty(text)) {
                        if (UOpts.WaitSearchQuery.is(user))
                            command = new Search(text, id, callbackId, user);
                        else if (UOpts.WaitFolderName.is(user))
                            command = new CreateDir(text, id, callbackId, user);
                        else if (UOpts.WaitFileName.is(user))
                            command = new RenameEntry(text, id, callbackId, user);
                        else
                            command = new CreateLabel(text, id, callbackId, user); // everything is a label!
                    } else
                        command = null;

                    CompletableFuture.runAsync(() -> hq.accept(command == null ? new RefreshView(id, callbackId, user) : command));
                } else
                    logger.debug("No UpdateRef object in request body");
            } else
                logger.debug("Empty request body");
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }
}
