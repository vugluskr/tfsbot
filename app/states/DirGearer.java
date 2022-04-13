package states;

import model.CommandType;
import model.MsgStruct;
import model.request.CallbackRequest;
import model.TUser;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import states.prompts.DropConfirmer;
import states.prompts.Locker;
import states.prompts.Renamer;
import states.prompts.Unlocker;
import utils.LangMap;

import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 16:43
 * tfs ☭ sweat and blood
 */
public class DirGearer extends AState {
    private int offset;

    public DirGearer(final UUID entryId) {
        this.entryId = entryId;
    }

    public DirGearer(final String encoded) {
        final int idx = encoded.indexOf(':');
        this.entryId = UUID.fromString(encoded.substring(0, idx));
        offset = getInt(encoded.substring(idx + 1));
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        if (request.getCommand().type != CommandType.setBooks)
            api.sendReaction(new BotApi.ReactionMessage(request.queryId, "", user.id));

        switch (request.getCommand().type) {
            case openLabel:
                return new LabelViewer(store.getSingleFolderLabel(entryId, offset + request.getCommand().elementIdx, user.id).getId());
            case rename:
                return new Renamer(entryId);
            case drop:
                return new DropConfirmer(entryId);
            case lock:
                return new Locker(entryId);
            case unlock:
                return new Unlocker(entryId);
            case setBooks:
                entry = store.getEntry(entryId, user);
                entry.setBookStore(!entry.isBookStore());
                api.sendReaction(new BotApi.ReactionMessage(request.queryId, LangMap.v(entry.isBookStore() ? LangMap.Value.IS_BOOK_STORE : LangMap.Value.IS_NOT_BOOK_STORE, user.lng,
                        entry.getName()), true, user.id));
                user.setBookStore(entry.isBookStore() ? entryId : null);
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
    public void display(final TUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        final int count = store.countFolderLabels(entryId, user.id);

        final MsgStruct struct = new MsgStruct();
        struct.body = escapeMd(v(LangMap.Value.GEARING, user, notNull(entry.getPath(), "/")));
        struct.mode = BotApi.ParseMode.Md2;
        struct.kbd = new BotApi.Keyboard();


        if (!user.getRoot().equals(entryId)) {
            if (entry.getOwner() == user.id)
                struct.kbd.button(entry.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
            if (entry.isRw())
                struct.kbd.button(CommandType.rename.b());
            if (entry.getOwner() == user.id) {
                struct.kbd.button(CommandType.setBooks.b());
                struct.kbd.button(CommandType.drop.b());
            }
        }
        struct.kbd.button(CommandType.cancel.b());

        pagedList(store.listFolderLabelsAsFiles(entryId, user.id, offset, 10), count, offset, struct);

        doSend(struct, user, api);
    }

    @Override
    public String encode() {
        return entryId.toString() + ":" + offset;
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return LangMap.Value.GEAR_HELP;
    }
}
