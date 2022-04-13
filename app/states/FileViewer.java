package states;

import model.CommandType;
import model.ContentType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.TUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import states.prompts.DropConfirmer;
import states.prompts.Locker;
import states.prompts.Renamer;
import states.prompts.Unlocker;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 18:20
 * tfs ☭ sweat and blood
 */
public class FileViewer extends AState {
    private boolean passwordIsOk;

    public FileViewer(final TFile entry) {
        this.entry = entry;
        this.entryId = entry.getId();
    }

    public FileViewer(final UUID entryId) {
        this.entryId = entryId;
    }

    public FileViewer(final String encoded) {
        final int idx = encoded.indexOf(':');
        this.entryId = UUID.fromString(encoded.substring(0, idx == -1 ? encoded.length() : idx));
        this.passwordIsOk = idx == -1 || encoded.charAt(idx + 1) == '1';
    }

    @Override
    public UserState onText(final TextRequest request, final TUser user, final BotApi api, final DataStore store) {
        entry = store.getEntry(entryId, user);

        if (entry.isLocked() && !passwordIsOk) {
            if (store.isPasswordOk(entryId, request.getText()))
                passwordIsOk = true;

            return null;
        }

        return new Searcher(entryId, request.getText());
    }

    @Override
    public UserState onFile(final FileRequest r, final TUser user, final BotApi api, final DataStore store) {
        if (r.isCrooked())
            return null;

        if (entry == null)
            entry = store.getEntry(entryId, user);

        if (entry.getOwner() != user.id)
            return null;

        entry.refId = r.getAttachNode().get("file_id").asText();

        store.updateEntry(entry);

        return null;
    }

    @Override
    public UserState voidOnCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case rename:
                return new Renamer(entryId);
            case drop:
                return new DropConfirmer(entryId);
            case share:
                return new EntrySharer(entryId);
            case lock:
                return new Locker(entryId);
            case unlock:
                return new Unlocker(entryId);
        }

        return null;
    }

    @Override
    public void display(final TUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        if (entry == null) {
            user.backHistory();
            user.state().display(user, api, store);
            return;
        }

        if (entry.isDir()) {
            new DirViewer(entryId).display(user, api, store);
        }

        if (entry.type == ContentType.SOFTLINK)
            entry = store.getEntry(UUID.fromString(entry.getRefId()), user);

        final MsgStruct struct = new MsgStruct();

        passwordIsOk = passwordIsOk || !entry.isLocked();

        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());

        if (!passwordIsOk)
            struct.body = "_" + escapeMd(LangMap.v(LangMap.Value.TYPE_PASSWORD_FILE, user, entry.getName())) + "_";
        else {
            if (entry.isRw()) {
                if (entry.getOwner() == user.id) {
                    struct.kbd.button(entry.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
                    struct.kbd.button(CommandType.share.b());
                }

                struct.kbd.button(CommandType.rename.b(), CommandType.drop.b());
            }

            struct.file = entry;
            struct.caption = escapeMd(entry.getPath());
        }

        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public String encode() {
        return entryId.toString() + ':' + (passwordIsOk ? '1' : '0');
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return LangMap.Value.FILE_HELP;
    }
}
