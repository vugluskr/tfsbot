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

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public class FileViewer extends ARole implements CallbackSink {
    private static final Logger.ALogger logger = Logger.of(FileViewer.class);

    private final int offset;
    private final String password;

    public FileViewer(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);

        offset = node != null && node.has("offset") ? node.get("offset").asInt() : 0;
        password = node != null && node.has("password") ? node.get("password").asText() : null;
    }

    @Override
    public void onCallback(final Command command) {
        switch (command.type) {
            case openParent:
                backToParent();
                break;
            case unlock:
                tfs.unlockEntry(tfs.get(entryId, user));
                doView();
                break;
            case lock:
                us.morphTo(Locker.class, user).doView();
                break;
            case share:
                us.morphTo(Sharer.class, user).doView();
                break;
            case renameFile:
                us.morphTo(Renamer.class, user).doView();
                break;
            case dropFile:
                tfs.rm(entryId, user);
                backToParent();
                break;
            default:
                logger.info("Нет обработчика для '" + command.type + "'");
                doView();
                break;
        }
    }

    private void backToParent() {
        try {
            entryId = tfs.get(entryId, user).getParentId();
        } catch (final Exception ignore) {
            entryId = user.rootId;
        }
        us.morphTo(DirViewer.class, user).doView();
    }

    @Override
    public void doView() {
        doView(tfs.get(entryId, user));
    }

    public void doView(final TFile entry) {
        this.entryId = entry.getId();

        if (entry.isFile()) {
            if (entry.isLocked()) {
                if (password == null)
                    us.morphTo(Unlocker.class, user).doView(entry);
                else if (tfs.passwordFailed(entryId, password)) {
                    entryId = entry.getParentId();
                    us.morphTo(DirViewer.class, user).doView();
                } else
                    doFileView(entry);
            } else
                doFileView(entry);
        } else if (entry.isLabel())
            us.morphTo(LabelViewer.class, user).doView(entry);
        else
            us.morphTo(DirViewer.class, user).doView(entry);
    }

    private void doFileView(final TFile file) {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();

        kbd.button(CommandType.openParent.b());

        if (file.isRw()) {
            if (file.getOwner() == user.id) {
                kbd.button(file.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
                kbd.button(CommandType.share.b());
            }

            kbd.button(CommandType.renameFile.b(), CommandType.dropFile.b());
        }

        api.sendContent(file, escapeMd(file.getPath()), ParseMode.md2, kbd, user);
    }

    @Override
    public JsonNode dump() {
        final ObjectNode parent = rootDump();

        parent.put("offset", offset);

        return parent;
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.FILE_HELP;
    }
}
