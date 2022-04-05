package states;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;
import utils.Strings;

import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 19:13
 * tfs ☭ sweat and blood
 */
public class EntrySharesGearer extends AState {
    private final UUID entryId;
    private int offset;

    private TFile entry;

    public EntrySharesGearer(final UUID entryId, final int offset) {
        this.entryId = entryId;
        this.offset = offset;
    }

    public EntrySharesGearer(final String encoded) {
        final int idx = encoded.indexOf(':');
        this.entryId = UUID.fromString(encoded.substring(0, idx));
        this.offset = getInt(encoded.substring(idx + 1));
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case dropGrant:
                store.dropEntryGrant(entryId, store.selectEntryGrants(entryId, offset, 10, user.id).get(request.getCommand().elementIdx));
                break;
            case rewind:
                offset -= 10;
                break;
            case forward:
                offset += 10;
                break;
        }

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user.id);

        final int count = store.countEntryGrants(entryId);

        final MsgStruct struct = new MsgStruct();

        final StringBuilder body = new StringBuilder(16);

        body.append(v(entry.isDir() ? LangMap.Value.DIR_ACCESS : LangMap.Value.FILE_ACCESS, user, "*" + escapeMd(entry.getPath()) + "*"));

        if (count == 0)
            body.append(Strings.Uni.People + ": _").append(escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user))).append("_");

        struct.body = body.toString();

        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());

        pagedList(store.selectEntryGrants(entryId, offset, 10, user.id), count, offset, struct, (share, idx) ->
                CommandType.dropGrant.b(Strings.Uni.drop + " " + share.getName(), idx));

        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public String encode() {
        return entryId.toString() + ":" + offset;
    }

    public LangMap.Value helpValue(final TgUser user) {
        return entry != null && entry.isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP;
    }
}
