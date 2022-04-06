package states.prompts;

import model.MsgStruct;
import model.TFile;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.AState;
import states.UserState;
import utils.LangMap;
import utils.TextUtils;

import java.math.BigInteger;
import java.util.UUID;

import static utils.TextUtils.escapeMd;
import static utils.TextUtils.hash256;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class Locker extends AState {
    public Locker(final UUID entryId) {
        this.entryId = entryId;
    }

    public Locker(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user);

        if (!entry.isLocked()) {
            final String salt = new BigInteger(130, TextUtils.rnd).toString(32);
            final String password = hash256(salt + request.getText());

            store.lockEntry(entry, salt, password);

            return _back;
        }

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user);
        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(entry.isDir() ? LangMap.Value.TYPE_LOCK_DIR : LangMap.Value.TYPE_LOCK_FILE, user, entry.getName()));
        struct.kbd = BotApi.voidKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.GEAR_HELP;
    }
}
