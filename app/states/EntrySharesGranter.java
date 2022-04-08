package states;

import model.ContentType;
import model.MsgStruct;
import model.TFile;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import model.user.UDbData;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;

import java.util.UUID;

import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getLong;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 19:30
 * tfs ☭ sweat and blood
 */
public class EntrySharesGranter extends AState {
    private TFile entry;

    public EntrySharesGranter(final UUID entryId) {
        this.entryId = entryId;
    }

    public EntrySharesGranter(final String encoded) {
        this.entryId = UUID.fromString(encoded);
    }

    @Override
    public String encode() {
        return entryId.toString();
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        final long id = getLong(request.getText());

        if (id > 0 && request.getText().matches("[0-9]+")) {
            final TFile file = new TFile();
            file.type = ContentType.CONTACT;
            file.setName("u" + id);
            file.setOwner(id);

            return onFile(file, user, store);
        }

        return null;
    }

    @Override
    public UserState onFile(final FileRequest request, final TgUser user, final BotApi api, final DataStore store) {
        return onFile(request.getFile(), user, store);
    }

    private UserState onFile(final TFile contact, final TgUser user, final DataStore store) {
        if (contact.type != ContentType.CONTACT || contact.getOwner() == user.id)
            return null;

        final long grantToId = contact.getOwner();

        final UDbData db = store.getUser(grantToId);
        final UUID rootId;
        if (db == null)
            rootId = store.initUserTables(grantToId);
        else {
            final int p = db.getS1().indexOf(':');
            rootId = UUID.fromString(db.getS1().substring(0, p < 0 ? db.getS1().length() : p));
        }

        final TgUser target = new TgUser(grantToId, rootId, contact.getName());

        if (db == null)
            store.insertUser(new UDbData(grantToId, rootId));

        if (store.entryNotGrantedTo(entryId, target.id, user.id))
            store.entryGrantTo(entryId, target, user);

        return _back;
    }


    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        final MsgStruct struct = new MsgStruct();
        struct.mode = BotApi.ParseMode.Md2;
        struct.body = escapeMd(LangMap.v(entry.isDir() ? LangMap.Value.SEND_CONTACT_DIR : LangMap.Value.SEND_CONTACT_FILE, user.lng, entry.getName()));
        struct.kbd = BotApi.voidKbd;

        doSend(struct, user, api);
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return entry != null && entry.isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP;
    }
}
