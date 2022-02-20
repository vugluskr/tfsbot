package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Command;
import model.CommandType;
import model.TFile;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import java.util.List;
import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class DirGearer extends APager<TFile> {
    private static final Logger.ALogger logger = Logger.of(DirGearer.class);

    private volatile TFile dir;
    private boolean lockPersist = false;

    public DirGearer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onCallback(final Command command) {
        if (notPagerCall(command))
            switch (command.type) {
                case unlock:
                    lockPersist = true;
                    us.morphTo(Unlocker.class, user).doView();
                    lockPersist = false;
                    break;
                case lock:
                    us.morphTo(Locker.class, user).doView();
                    break;
                case share:
                    us.morphTo(Sharer.class, user).doView();
                    break;
                case renameDir:
                    us.morphTo(Renamer.class, user).doView();
                    break;
                case dropDir:
                    final UUID tmp = tfs.get(entryId, user).getParentId();
                    tfs.rm(entryId, user);
                    entryId = tmp;
                case cancel:
                    us.morphTo(DirViewer.class, user).doView();
                    break;
                case openLabel:
                    if (dir == null)
                        dir = tfs.get(entryId, user);

                    if (dir.isSharesRoot())
                        us.morphTo(ShareViewer.class, user).doView(tfs.getGearShareEntry(user.id, command.elementIdx, this));
                    else
                        us.morphTo(LabelViewer.class, user).doView(tfs.getGearEntry(entryId, command.elementIdx, this));
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
    public LangMap.Value helpValue() {
        return LangMap.Value.GEAR_HELP;
    }

    @Override
    protected int prepareCountScope() {
        if (dir == null)
            dir = tfs.get(entryId, user);

        if (dir.isSharesRoot())
            return tfs.countGearShares(user.id);

        return tfs.countDirLabels(entryId, user.id);
    }

    @Override
    protected TgApi.Button toButton(final TFile element, final int withIdx) {
        return element.toButton(withIdx);
    }

    @Override
    protected List<TFile> selectScope(final int offset, final int limit) {
        if (dir.isSharesRoot())
            return tfs.gearShares(user.id, user.lang, offset, limit);

        return tfs.gearFolder(entryId, user.id, offset, limit);
    }

    @Override
    protected String initBody(final boolean noElements) {
        return escapeMd(v(LangMap.Value.GEARING, user, notNull(dir.getPath(), "/")));
    }

    @Override
    protected TgApi.Keyboard initKeyboard() {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        if (isDeep() && !dir.isSharesRoot()) {
            if (dir.getOwner() == user.id) {
                kbd.button(dir.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
                kbd.button(CommandType.share.b());
            }
            if (dir.isRw())
                kbd.button(CommandType.renameDir.b());
            if (dir.getOwner() == user.id)
                kbd.button(CommandType.dropDir.b());
        }

        kbd.button(CommandType.cancel.b());

        return kbd;
    }

    @Override
    public JsonNode dump() {
        final ObjectNode node = rootDump();
        node.put("persist", lockPersist);

        return node;
    }

    @Override
    protected String offName() {
        return "gear_offset";
    }
}
