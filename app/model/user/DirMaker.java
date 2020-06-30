package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.TFile;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;
import utils.TFileFactory;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class DirMaker extends ARole implements InputSink, CallbackSink {
    public DirMaker(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onInput(String input) {
        if (!isEmpty(input) && tfs.entryMissed((input = input.replace('/', '_')), user)) {
            final TFile dir = tfs.mk(TFileFactory.dir(input, entryId, user.id));

            entryId = dir.getId();
        }

        us.morphTo(DirViewer.class, user).doView();
    }

    @Override
    public void onCallback(final Command command) {
        us.morphTo(DirViewer.class, user).doView();
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.LS_HELP;
    }

    @Override
    public void doView() {
        api.dialog(LangMap.Value.TYPE_FOLDER, user);
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }
}
