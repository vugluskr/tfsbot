package states.prompts;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 17:40
 * tfs ☭ sweat and blood
 */
public class DropConfirmer extends AState {
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
    public UserState voidOnCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        if (request.getCommand().type == CommandType.confirm) {
            store.rm(entryId, user);
            user.clearHistoryTail(entryId);
        }

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user);

        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(entry.isDir() ? LangMap.Value.CONFIRM_DROP_DIR : LangMap.Value.CONFIRM_DROP_FILE, user, entry.getName()));
        struct.kbd = BotApi.yesNoKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }
}
