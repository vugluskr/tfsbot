package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.ContentType;
import model.TFile;
import model.User;
import play.Logger;
import play.libs.Json;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.*;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.06.2020
 * tfs ☭ sweat and blood
 */
public class ShareGranter extends ARole implements CallbackSink, InputSink {
    private static final Logger.ALogger logger = Logger.of(ShareGranter.class);

    public ShareGranter(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);
    }

    @Override
    public void onCallback(final Command command) {
        us.morphTo(Sharer.class, user).doView();
    }

    @Override
    public JsonNode dump() {
        return rootDump();
    }

    @Override
    public LangMap.Value helpValue() {
        return tfs.get(entryId, user).isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP;
    }

    @Override
    public void doView() {
        final TFile entry = tfs.get(entryId, user);
        api.dialog(entry.isDir() ? LangMap.Value.SEND_CONTACT_DIR : LangMap.Value.SEND_CONTACT_FILE, user, entry.getName());
    }

    public void onInput(final String input) {
        if (!isEmpty(input)) {
            final long id = getLong(input);

            if (id > 0 && input.matches("[0-9]+")) {
                final TFile file = new TFile();
                file.type = ContentType.CONTACT;
                file.name = "u" + id;
                file.setOwner(id);

                onFile(file);
                return;
            } else
                logger.info("Unknown input: " + input);
        }

        super.doSearch(input);
    }

    @Override
    public void onFile(final TFile contact) {
        if (contact.type != ContentType.CONTACT)
            super.onFile(contact);
        else {
            final User target = us.resolveUser(contact.getOwner(), user.lang, contact.name); // create user

            if (target == null || target.id == user.id) {
                doView();
                return;
            }

            if (tfs.entryNotGrantedTo(entryId, target.id, user.id))
                tfs.entryGrantTo(entryId, target, target.name, user);

            us.morphTo(Sharer.class, user).doView();
        }
    }
}
