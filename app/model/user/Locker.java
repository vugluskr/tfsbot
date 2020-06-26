package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.TFile;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;
import utils.TextUtils;

import java.math.BigInteger;

import static utils.TextUtils.hash256;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class Locker extends ARole implements InputSink, CallbackSink {
    public Locker(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onCallback(final Command command) {
        final TFile entry = tfs.get(entryId, user);

        final Class<? extends Role> role = entry.isFile() ? FileViewer.class : DirGearer.class;
        us.morphTo(role, user).doView();
    }

    @Override
    public void onInput(final String input) {
        final TFile entry = tfs.get(entryId, user);

        if (!isEmpty(input) && !entry.isLocked()) {
            final String salt = new BigInteger(130, TextUtils.rnd).toString(32);
            final String password = hash256(salt + input);

            tfs.lockEntry(entry, salt, password);
        }

        if (entry.isFile())
            us.morphTo(FileViewer.class, user).doView(entry);
        else
            us.morphTo(DirViewer.class, user).doView(entry);
    }

    @Override
    public LangMap.Value helpValue() {
        return tfs.get(entryId, user).isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP;
    }

    @Override
    public void doView() {
        final TFile entry = tfs.get(entryId, user);

        api.dialog(entry.isDir() ? LangMap.Value.TYPE_LOCK_DIR : LangMap.Value.TYPE_LOCK_FILE, user, entry.getName());
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }
}
