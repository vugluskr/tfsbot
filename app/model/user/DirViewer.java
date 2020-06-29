package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.CommandType;
import model.TFile;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import java.util.List;
import java.util.Objects;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class DirViewer extends APager<TFile> {
    private static final Logger.ALogger logger = Logger.of(DirViewer.class);

    private final String password;

    private volatile TFile dir;

    public DirViewer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);

        password = node != null && node.has("password") ? node.get("password").asText() : null;
    }

    @Override
    public void onCallback(final Command command) {
        if (notPagerCall(command))
            switch (command.type) {
                case openParent:
                    entryId = tfs.get(entryId, user).getParentId();
                    doView();
                    break;
                case mkDir:
                    us.morphTo(DirMaker.class, user).doView();
                    break;
                case mkLabel:
                    us.morphTo(LabelMaker.class, user).doView();
                    break;
                case openDir:
                    doView(tfs.getFolderEntry(entryId, command.elementIdx, this));
                    break;
                case openFile:
                    us.morphTo(FileViewer.class, user).doView(tfs.getFolderEntry(entryId, command.elementIdx, this));
                    break;
                case gear:
                    us.morphTo(DirGearer.class, user).doView();
                    break;
                default:
                    logger.info("Нет обработчика для '" + command.type + "'");
                    restart();
                    break;
            }
    }

    @Override
    public LangMap.Value helpValue() {
        return isDeep() ? LangMap.Value.LS_HELP : LangMap.Value.ROOT_HELP;
    }

    public void doView(final TFile entry) {
        if (entry != null && entry.isDir() && !entryId.equals(entry.getId())) {
            this.entryId = entry.getId();
            scopeChanged();
            dir = entry;
        }

        doView();
    }

    @Override
    protected boolean isViewAllowed() {
        prepareCountScope();

        if (dir == null) {
            if (Objects.equals(entryId, user.rootId))
                tfs.reinitUserTables(user.id);

            us.resolveUser(user.id, user.lang, user.name).start();
//            restart();
            return false;
        }

        if (dir.isLocked()) {
            if (password == null) {
                us.morphTo(Unlocker.class, user).doView(dir);
                return false;
            }

            if (tfs.passwordFailed(entryId, password)) {
                entryId = dir.getParentId();
                doView(tfs.get(dir.getParentId(), user));
                return false;
            }
        }

        return true;
    }

    @Override
    protected int prepareCountScope() {
        if (dir == null)
            dir = tfs.get(entryId, user);

        return tfs.countFolder(entryId, user.id);
    }

    @Override
    protected TgApi.Button toButton(final TFile element, final int withIdx) {
        return element.toButton(withIdx);
    }

    @Override
    protected List<TFile> selectScope(final int offset, final int limit) {
        return tfs.listFolder(entryId, offset, limit, user.id);
    }

    @Override
    protected String initBody(final boolean noElements) {
        final List<String> labels = tfs.listLabels(entryId, user.id);
        final StringBuilder body = new StringBuilder(0);
        body.append(notNull(escapeMd(dir.getPath()), "/"));

        final StringBuilder ls = new StringBuilder();
        labels.forEach(l -> ls.append('\n').append("```\n").append(escapeMd(l)).append("```\n"));

        if (ls.length() > 0)
            body.append(ls.toString());
        else if (noElements)
            body.append("\n_").append(escapeMd(v(LangMap.Value.NO_CONTENT, user))).append("_");

        dir = null;
        return body.toString();
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }

    @Override
    protected TgApi.Keyboard initKeyboard() {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        if (isDeep())
            kbd.button(CommandType.openParent.b());
        if (dir.isRw()) {
            kbd.button(CommandType.mkLabel.b());
            kbd.button(CommandType.mkDir.b());
            kbd.button(CommandType.gear.b());
        }

        return kbd;
    }
}
