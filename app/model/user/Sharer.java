package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.CommandType;
import model.Share;
import model.TFile;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;
import utils.Strings;

import java.util.List;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class Sharer extends APager<Share> {
    private static final Logger.ALogger logger = Logger.of(Sharer.class);

    private volatile TFile subject;
    private volatile Share glob;
    private volatile boolean gearing;

    public Sharer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    protected int prepareCountScope() {
        subject = tfs.get(entryId, user);
        glob = tfs.getEntryLink(entryId);

        return tfs.countEntryGrants(entryId);
    }

    @Override
    protected TgApi.Button toButton(final Share s, final int withIdx) {
        if (gearing)
            return CommandType.dropGrant.b(Strings.Uni.drop + " " + s.getName(), withIdx);

        return CommandType.changeGrantRw.b(v(s.isReadWrite() ? LangMap.Value.SHARE_RW : LangMap.Value.SHARE_RO, user, s.getName()), withIdx);
    }

    @Override
    protected List<Share> selectScope(final int offset, final int limit) {
        return tfs.selectEntryGrants(entryId, offset, limit, user.id);
    }

    @Override
    protected String initBody(final boolean noElements) {
        final StringBuilder body = new StringBuilder(16);

        body.append(v(subject.isDir() ? LangMap.Value.DIR_ACCESS : LangMap.Value.FILE_ACCESS, user, "*" + escapeMd(subject.getPath()) + "*"))
                .append("\n\n")
                .append(Strings.Uni.link).append(": _")
                .append(escapeMd((glob != null ? "https://t.me/" + us.getBotName() + "?start=shared-" + glob.getId() : v(LangMap.Value.NO_GLOBAL_LINK, user))))
                .append("_\n");

        if (noElements)
            body.append(Strings.Uni.People + ": _").append(escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user))).append("_");

        return body.toString();
    }

    @Override
    protected TgApi.Keyboard initKeyboard() {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        kbd.button(glob != null ? CommandType.dropEntryLink.b() : CommandType.makeEntryLink.b());
        kbd.button(CommandType.mkGrant.b());
        if (!gearing)
            kbd.button(CommandType.gear.b());
        kbd.button(CommandType.cancel.b());

        return kbd;
    }

    @Override
    public void onCallback(final Command command) {
        if (notPagerCall(command))
            switch (command.type) {
                case changeGrantRw:
                    tfs.changeEntryGrantRw(entryId, command.elementIdx, this);
                    doView();
                    break;
                case dropEntryLink:
                    tfs.dropEntryLink(entryId, user.id);
                    doView();
                    break;
                case makeEntryLink:
                    tfs.makeEntryLink(entryId, user);
                    doView();
                    break;
                case mkGrant:
                    us.morphTo(ShareGranter.class, user).doView();
                    break;
                case cancel:
                    final TFile file = tfs.get(entryId, user);
                    if (file.isDir())
                        us.morphTo(DirGearer.class, user).doView();
                    else
                        us.morphTo(FileViewer.class, user).doView(file);
                    break;
                case dropGrant:
                    tfs.dropEntryGrant(entryId, command.elementIdx, this);
                    doView();
                    break;
                case gear:
                    gearing = true;
                    doView();
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
        return rootDump();
    }

    @Override
    public LangMap.Value helpValue() {
        return tfs.get(entryId, user).isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP;
    }
}
