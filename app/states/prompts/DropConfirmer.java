package states.prompts;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.AState;
import states.UserState;
import utils.LangMap;
import utils.TFileFactory;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 17:40
 * tfs ☭ sweat and blood
 */
public class DropConfirmer extends AState {
    private final UUID entryId;

    public DropConfirmer(final UUID entryId) {
        this.entryId = entryId;
    }

    public DropConfirmer(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.GEAR_HELP;
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        if (request.getCommand().type == CommandType.confirm)
            store.rm(entryId, user);

        return _back;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user.id);

        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(entry.isDir() ? LangMap.Value.CONFIRM_DROP_DIR : LangMap.Value.CONFIRM_DROP_FILE, user, entry.getName()));
        struct.kbd = BotApi.yesNoKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }
}
