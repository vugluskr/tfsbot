package utils;

import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.TeleFile;
import model.telegram.api.UpdateRef;
import model.telegram.commands.*;
import play.libs.Json;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

public class Test {
    static final String string = "{\"update_id\":125899998,\"callback_query\":{\"id\":\"297094852152425184\",\"from\":{\"id\":69172785,\"is_bot\":false,\"first_name\":\"Denis\"," +
            "\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"language_code\":\"ru\"},\"message\":{\"message_id\":1332,\"from\":{\"id\":1186923184,\"is_bot\":true,\"first_name\":\"TFS\",\"username\":\"telefsBot\"},\"chat\":{\"id\":69172785,\"first_name\":\"Denis\",\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"type\":\"private\"},\"date\":1589476100,\"edit_date\":1589633647,\"text\":\"/\",\"reply_markup\":{\"inline_keyboard\":[[{\"text\":\"\uD83C\uDFF7\",\"callback_data\":\"lb_1\"},{\"text\":\"\uD83D\uDD0D\",\"callback_data\":\"sr\"},{\"text\":\"\uD83D\uDCC1\",\"callback_data\":\"mk_\"},{\"text\":\"⚙\",\"callback_data\":\"ed_\"}],[{\"text\":\"\uD83D\uDCC2  health\",\"callback_data\":\"cd_24\"}],[{\"text\":\"\uD83D\uDCC2  soft\",\"callback_data\":\"cd_122\"}],[{\"text\":\"\uD83D\uDCC2  всякое\",\"callback_data\":\"cd_139\"}],[{\"text\":\"\uD83D\uDCC2  доки\",\"callback_data\":\"cd_146\"}],[{\"text\":\"\uD83D\uDCC2  наш_дом\",\"callback_data\":\"cd_114\"}],[{\"text\":\"обезьяна стикер\",\"callback_data\":\"gt_9\"}]]}},\"chat_instance\":\"8786880905295357662\",\"data\":\"cd_122\"}}";

    public static void main(final String[] argz) throws Exception {
        final UpdateRef updateRef = Json.fromJson(Json.parse(string), UpdateRef.class);

        if (updateRef != null) {
            final User user = new User();

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

            System.out.println(command);
        }
    }
}
