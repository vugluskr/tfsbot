package states;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
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
    private final UUID entryId;

    private TFile entry;

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
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user.id);

        final MsgStruct struct = new MsgStruct();
        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());

        if (entry.isRw())
            struct.kbd.button(CommandType.editLabel.b(), CommandType.dropLabel.b());

        struct.mode = BotApi.ParseMode.Md2;
        struct.body = notNull(escapeMd(entry.parentPath()), "/")+"*\n\n"+escapeMd(entry.name);

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.LABEL_HELP;
    }
}
