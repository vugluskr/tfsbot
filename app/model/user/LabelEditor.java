package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.TFile;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.06.2020
 * tfs ☭ sweat and blood
 */
public class LabelEditor extends ARole implements InputSink {

    @SuppressWarnings("unused")
    public LabelEditor(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onInput(String input) {
        final TFile label = tfs.get(entryId, user);

        if (!isEmpty(input) && tfs.entryMissed(input, user) && !notNull(input).equals(label.getName())) {
            label.setName(input);
            tfs.updateMeta(label, user);
        }

        us.morphTo(LabelViewer.class, user).doView(label);
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
