package states;

import model.MsgStruct;
import model.TFile;
import model.request.TextRequest;
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
 * 05.04.2022 17:20
 * tfs ☭ sweat and blood
 */
public class LabelEditor extends AState {
    public LabelEditor(final UUID entryId) {
        this.entryId = entryId;
    }

    public LabelEditor(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    protected String encode() {
        return entryId.toString();
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        if (store.isEntryMissed(entryId, request.getText(), user)) {
            final TFile entry = store.getEntry(entryId, user);
            entry.setName(request.getText());
            store.updateEntry(entry);
        }

        return _back;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(LangMap.Value.TYPE_EDIT_LABEL, user));
        struct.kbd = BotApi.voidKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.LABEL_HELP;
    }
}
