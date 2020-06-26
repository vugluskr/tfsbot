package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.TFile;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import java.nio.file.Paths;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.06.2020
 * tfs ☭ sweat and blood
 */
public class Renamer extends ARole implements InputSink, CallbackSink {
    public Renamer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
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

        if (!entry.getName().equals(input) && tfs.entryMissed(input, user)) {
            entry.setName(input);
            entry.setPath(Paths.get(entry.getPath()).getParent().resolve(input).toString());
            tfs.updateMeta(entry, user);
        }

        if (entry.isDir())
            us.morphTo(DirViewer.class, user).doView(entry);
        else
            us.morphTo(FileViewer.class, user).doView(entry);
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }

    @Override
    public LangMap.Value helpValue() {
        return tfs.get(entryId, user).isDir() ? LangMap.Value.LS_HELP : LangMap.Value.FILE_HELP;
    }

    @Override
    public void doView() {
        api.dialog(LangMap.Value.TYPE_RENAME, user, tfs.get(entryId, user).getName());
    }
}
