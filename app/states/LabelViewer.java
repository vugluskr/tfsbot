package states;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.TUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 18:37
 * tfs ☭ sweat and blood
 */
public class LabelViewer extends AState {
    public LabelViewer(final TFile entry) {
        this.entry = entry;
        this.entryId = entry.getId();
    }

    public LabelViewer(final UUID entryId) {
        this.entryId = entryId;
    }

    public LabelViewer(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public UserState voidOnCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case rename:
                return new LabelEditor(entryId);
            case drop:
                store.rm(entryId, user);
                return _back;
        }

        return null;
    }

    @Override
    public void display(final TUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        final MsgStruct struct = new MsgStruct();
        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());

        if (entry.isRw())
            struct.kbd.button(CommandType.rename.b(), CommandType.drop.b());

//        struct.mode = BotApi.ParseMode.Md2;
        struct.body = /*"*" +*/ notNull(escapeMd(entry.parentPath()), "/") + "\n\n" + escapeMd(entry.getName());

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return LangMap.Value.LABEL_HELP;
    }
}
