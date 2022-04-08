package states.prompts;

import model.MsgStruct;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;
import utils.TFileFactory;

import java.util.UUID;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 16:45
 * tfs ☭ sweat and blood
 */
public class LabelMaker extends AState {
    public LabelMaker(final UUID entryId) {
        this.entryId = entryId;
    }

    public LabelMaker(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.LABEL_HELP;
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        if (store.isEntryMissed(entryId, request.getText(), user))
            store.mk(TFileFactory.label(request.getText(), entryId, user.id));

        return _back;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(LangMap.v(LangMap.Value.TYPE_LABEL, user));
        struct.kbd = BotApi.voidKbd;
        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }
}
