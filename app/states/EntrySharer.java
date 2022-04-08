package states;

import model.CommandType;
import model.MsgStruct;
import model.Share;
import model.request.CallbackRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;
import utils.Strings;

import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 18:50
 * tfs ☭ sweat and blood
 */
public class EntrySharer extends AState {
    private int offset;

    public EntrySharer(final UUID entryId) {
        this.entryId = entryId;
    }

    public EntrySharer(final String encoded) {
        final int idx = encoded.indexOf(':');
        this.entryId = UUID.fromString(encoded.substring(0, idx));
        offset = getInt(encoded.substring(idx + 1));
    }

    @Override
    public UserState voidOnCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case dropEntryLink:
                store.dropEntryLink(entryId, user.id);
                break;
            case makeEntryLink:
                store.makeEntryLink(entryId, user);
                break;
            case mkGrant:
                return new EntrySharesGranter(entryId);
            case gearDir:
                return new EntrySharesGearer(entryId, offset);
            case changeGrantRw:
                store.changeEntryGrantRw(entryId, request.getCommand().elementIdx, offset, user.id);
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
            entry = store.getEntry(entryId, user);

        final int count = store.countEntryGrants(entryId);

        final MsgStruct struct = new MsgStruct();

        final Share glob = store.getEntryLink(entryId);

        final StringBuilder body = new StringBuilder(16);

        body.append(v(entry.isDir() ? LangMap.Value.DIR_ACCESS : LangMap.Value.FILE_ACCESS, user, "*" + escapeMd(entry.getPath()) + "*"))
                .append("\n\n")
                .append(Strings.Uni.link).append(": _")
                .append(escapeMd((glob != null ? "https://t.me/" + store.getBotName() + "?start=shared-" + glob.getId() : v(LangMap.Value.NO_GLOBAL_LINK, user))))
                .append("_\n");

        if (count == 0)
            body.append(Strings.Uni.People + ": _").append(escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user))).append("_");

        struct.body = body.toString();

        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());
        struct.kbd.button(glob != null ? CommandType.dropEntryLink.b() : CommandType.makeEntryLink.b());
        struct.kbd.button(CommandType.mkGrant.b());
        if (count > 0)
            struct.kbd.button(CommandType.gearDir.b());

        pagedList(store.selectEntryGrants(entryId, offset, 10, user.id), count, offset, struct, (share, idx) ->
                CommandType.changeGrantRw.b(LangMap.v(share.isReadWrite() ? LangMap.Value.SHARE_RW : LangMap.Value.SHARE_RO, user.lng, share.getName()), idx));

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
