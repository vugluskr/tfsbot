package model.user;

import com.fasterxml.jackson.databind.JsonNode;
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
public class LabelMaker extends ARole implements InputSink {

    public LabelMaker(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onInput(String input) {
        if (!isEmpty(input) && tfs.entryMissed(input, user))
            tfs.mk(TFileFactory.label(input, entryId, user.id));

        us.morphTo(DirViewer.class, user).doView();
    }

    @Override
    public void doView() {
        api.dialog(LangMap.Value.TYPE_LABEL, user);
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.LABEL_HELP;
    }
}
