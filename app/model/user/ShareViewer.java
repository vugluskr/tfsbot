package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Command;
import model.CommandType;
import model.ParseMode;
import model.Share;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 20.02.2022 16:56
 * tfs ☭ sweat and blood
 */
public class ShareViewer extends ARole implements CallbackSink {
    private static final Logger.ALogger logger = Logger.of(LabelViewer.class);

    private String shareId;

    public ShareViewer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
        shareId = node != null && node.has("shareId") ? node.get("shareId").asText() : "";
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.LABEL_HELP;
    }

    @Override
    public void onCallback(final Command command) {
        switch (command.type) {
            case openParent:
                us.morphTo(DirGearer.class, user).doView();
                break;
            case dropLabel:
                tfs.unshareFromMe(shareId, user);
                us.morphTo(DirGearer.class, user).doView();
                break;
            case Void:
                user.doView();
                break;
            default:
                logger.info("Нет обработчика для '" + command.type.name() + "'");
                us.reset(user);
                user.doView();
                break;
        }
    }

    @Override
    public JsonNode dump() {
        final ObjectNode data = rootDump();

        data.put("shareId", shareId);

        return data;
    }

    @Override
    public void doView() {
        doView(tfs.getShare(shareId));
    }

    public void doView(final Share share) {
        this.shareId = share.getId();
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        kbd.button(CommandType.openParent.b());
        kbd.button(CommandType.dropLabel.b());

        api.sendContent(null, escapeMd(share.getName()), ParseMode.md2, kbd, user);
    }
}
