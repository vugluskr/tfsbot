package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Command;
import model.CommandType;
import model.ParseMode;
import model.TFile;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class LabelViewer extends ARole implements CallbackSink {
    private static final Logger.ALogger logger = Logger.of(LabelViewer.class);

    @SuppressWarnings("unused")
    public LabelViewer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.LABEL_HELP;
    }

    @Override
    public void onCallback(final Command command) {
        switch (command.type) {
            case openParent:
                us.morphTo(DirViewer.class, user).doView(tfs.getParentOf(entryId, user));
                break;
            case editLabel:
                us.morphTo(LabelEditor.class, user).doView();
                break;
            case dropLabel:
                final TFile dir = tfs.getParentOf(entryId, user);
                tfs.rm(entryId, user);
                us.morphTo(DirViewer.class, user).doView(dir);
                break;
            default:
                logger.info("Нет обработчика для '" + command.type + "'");
                us.reset(user);
                user.doView();
                break;
        }
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }

    @Override
    public void doView() {
        doView(tfs.get(entryId, user));
    }

    public void doView(final TFile label) {
        this.entryId = label.getId();
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        kbd.button(CommandType.openParent.b());

        if (label.isRw())
            kbd.button(CommandType.editLabel.b(), CommandType.dropLabel.b());

        api.sendContent(null, "*" + notNull(escapeMd(label.parentPath()), "/")+"*\n\n"+escapeMd(label.name), ParseMode.md2, kbd, user);
    }
}
