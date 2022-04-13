package states.prompts;

import model.MsgStruct;
import model.TFile;
import model.request.TextRequest;
import model.TUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 18:02
 * tfs ☭ sweat and blood
 */
public class Unlocker extends AState {
    public Unlocker(final TFile entry) {
        this.entry = entry;
        this.entryId = entry.getId();
    }

    public Unlocker(final UUID entryId) {
        this.entryId = entryId;
    }

    public Unlocker(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    protected String encode() {
        return entryId.toString();
    }

    @Override
    public UserState onText(final TextRequest request, final TUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        if (entry.isLocked() && store.isPasswordOk(entryId, request.getText())) {
            store.unlockEntry(entry);

            return _back;
        }

        return null;
    }

    @Override
    public void display(final TUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);
        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(entry.isDir() ? LangMap.Value.TYPE_PASSWORD_DIR : LangMap.Value.TYPE_PASSWORD_FILE, user, entry.getName()));
        struct.kbd = BotApi.voidKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return LangMap.Value.GEAR_HELP;
    }
}
