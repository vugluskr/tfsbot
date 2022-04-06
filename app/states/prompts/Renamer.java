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

import java.nio.file.Paths;
import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 17:33
 * tfs ☭ sweat and blood
 */
public class Renamer extends AState {
    public Renamer(final UUID entryId) {
        this.entryId = entryId;
    }

    public Renamer(final String encoded) {
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
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user);

        if (!entry.getName().equals(request.getText()) && store.isEntryMissed(entry.getParentId(), request.getText(), user)) {
            entry.setName(request.getText());
            entry.setPath(Paths.get(entry.getPath()).getParent().resolve(request.getText()).toString());
            store.updateEntry(entry);

            return _back;
        }

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(entryId, user);
        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(LangMap.Value.TYPE_RENAME, user, entry.getName()));
        struct.kbd = BotApi.voidKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }
}
