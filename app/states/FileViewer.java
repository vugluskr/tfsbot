package states;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 18:20
 * tfs ☭ sweat and blood
 */
public class FileViewer extends AState {
    private final UUID entryId;
    private boolean passwordIsOk;

    private TFile entry;

    public FileViewer(final TFile entry) {
        this.entry = entry;
        this.entryId = entry.getId();
    }

    public FileViewer(final UUID entryId) {
        this.entryId = entryId;
    }

    public FileViewer(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        entry = store.getEntry(entryId, user.id);

        if (entry.isLocked() && !passwordIsOk) {
            if (store.isPasswordOk(entryId, request.getText()))
                passwordIsOk = true;

            return null;
        }

        return new Searcher(entryId, request.getText());
    }

    @Override
    public UserState onFile(final FileRequest r, final TgUser user, final BotApi api, final DataStore store) {
        if (r.isCrooked())
            return null;

        if (entry == null)
            entry = store.getEntry(entryId, user.id);

        if (entry.getOwner() != user.id)
            return null;

        entry.refId = r.getAttachNode().get("file_id").asText();

        store.updateEntry(entry);

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user.id);

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

                struct.kbd.button(CommandType.renameFile.b(), CommandType.dropFile.b());
            }

            struct.file = entry;
            struct.caption = escapeMd(entry.getPath());
        }

        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.FILE_HELP;
    }
}
