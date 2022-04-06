package states;

import com.fasterxml.jackson.databind.JsonNode;
import model.CommandType;
import model.ContentType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import states.prompts.DirMaker;
import states.prompts.LabelMaker;
import utils.LangMap;

import java.util.List;
import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:31
 * tfs ☭ sweat and blood
 */
public class DirViewer extends AState {
    private int offset;
    private boolean passwordIsOk;

    public DirViewer(final TFile entry) {
        this.entryId = entry.getId();
        this.entry = entry;
    }

    public DirViewer(final UUID entryId) {
        this.entryId = entryId;
    }

    public DirViewer(final String stored) {
        for (int i = 0, from = 0, idx = 0; i < stored.length(); i++)
            if (stored.charAt(i) == ':') {
                final String s = stored.substring(from, i);
                from = i + 1;
                switch (idx++) {
                    case 0:
                        entryId = UUID.fromString(s);
                        break;
                    case 1:
                        offset = getInt(s);
                        passwordIsOk = stored.charAt(i+1) == '1';
                        return;
                }
            }
    }

    @Override
    public String encode() {
        return entryId + ":" + offset + ":" + (passwordIsOk ? '1' : '0');
    }

    @Override
    public UserState onFile(final FileRequest r, final TgUser user, final BotApi api, final DataStore store) {
        if (r.isCrooked())
            return null;

        if (entry == null)
            entry = store.getEntry(entryId, user);

        if (!entry.isRw())
            return null;

        final TFile file = r.getFile();
        final JsonNode attachNode = r.getAttachNode();

        if (file.refId == null) file.refId = attachNode.get("file_id").asText();
        if (file.uniqId == null) file.uniqId = attachNode.get("file_unique_id").asText();

        if (isEmpty(file.getName()))
            file.setName(r.getSrcNode().has("caption") && !r.getSrcNode().get("caption").asText().trim().isEmpty()
                    ? r.getSrcNode().get("caption").asText().trim()
                    : file.type.name().toLowerCase() + "_" + file.uniqId);

        if (file.type == ContentType.CONTACT)
            file.refId = attachNode.toString();

        file.setParentId(entryId);
        file.setOwner(r.user.id);

        store.mk(file);

        return null;
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return entryId.equals(user.getRoot()) ? LangMap.Value.ROOT_HELP : LangMap.Value.LS_HELP;
    }

    @Override
    public UserState voidOnCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case openDir:
                return new DirViewer(store.getSingleFolderEntry(entryId, request.getCommand().elementIdx + offset, user.id));
            case openFile:
                return new FileViewer(store.getSingleFolderEntry(entryId, request.getCommand().elementIdx + offset, user.id));
            case mkLabel:
                return new LabelMaker(entryId);
            case mkDir:
                return new DirMaker(entryId);
            case gearDir:
                return new DirGearer(entryId);
            case rewind:
                offset -= 10;
                break;
            case forward:
                offset += 10;
                break;
        }

        return null;
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        entry = store.getEntry(entryId, user);

        if (entry.isLocked() && !passwordIsOk) {
            if (store.isPasswordOk(entryId, request.getText()))
                passwordIsOk = true;

            return null;
        }

        return new Searcher(entryId, request.getText());
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (entry == null)
            entry = store.getEntry(entryId, user);

        final MsgStruct struct = new MsgStruct();

        passwordIsOk = passwordIsOk || !entry.isLocked();

        struct.kbd = new BotApi.Keyboard();

        if (!user.getRoot().equals(entryId))
            struct.kbd.button(CommandType.goBack.b());

        if (!passwordIsOk)
            struct.body = "_" + escapeMd(LangMap.v(LangMap.Value.TYPE_PASSWORD_DIR, user, entry.getName())) + "_";
        else {
            final int count = store.countFolder(entryId, user.id);

            final List<String> labels = store.listFolderLabels(entryId, user.id);

            final StringBuilder ls = new StringBuilder();
            labels.forEach(l -> ls.append('\n').append("```\n").append(escapeMd(l)).append("```\n"));

            final StringBuilder body = new StringBuilder(0);
            body.append(notNull(escapeMd(entry.getPath()), "/"));

            if (ls.length() > 0)
                body.append(ls);
            else if (count <= 0)
                body.append("\n_").append(escapeMd(v(entryId.equals(user.getRoot()) ? LangMap.Value.NO_CONTENT_START : LangMap.Value.NO_CONTENT, user))).append("_");

            struct.body = body.toString();

            if (entry.isRw()) {
                struct.kbd.button(CommandType.mkLabel.b());
                struct.kbd.button(CommandType.mkDir.b());
                struct.kbd.button(CommandType.gearDir.b());
            }

            pagedList(store.listFolder(entryId, offset, 10, user), count, offset, struct);
        }

        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

}
