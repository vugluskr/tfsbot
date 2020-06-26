package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Share;
import model.TFile;
import model.User;
import play.libs.Json;
import services.TfsService;
import services.TgApi;
import services.UserService;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.06.2020
 * tfs ☭ sweat and blood
 */
public abstract class ARole implements Role {
    protected final TfsService tfs;
    protected final TgApi api;
    protected final UserService us;

    public UUID entryId;
    public User user;

    public ARole(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        this.tfs = tfs;
        this.api = api;
        this.us = us;

        entryId = node == null || !node.has("entryId") ? null : UUID.fromString(node.get("entryId").asText());
    }

    public final boolean isDeep() {
        return !user.rootId.equals(entryId);
    }

    protected ObjectNode rootDump() {
        final ObjectNode data = Json.newObject();

        data.put("entryId", (entryId == null ? user.rootId : entryId).toString());

        return data;
    }

    public void onFile(final TFile upload) {
        final TFile entry = tfs.get(entryId, user);

        upload.setParentId(entry.isDir() ? entry.getId() : entry.getParentId());
        upload.setOwner(user.id);

        tfs.mk(upload);

        entryId = entry.isDir() ? entryId : entry.getParentId();
        us.morphTo(DirViewer.class, user).doView();
    }

    public void doSearch(final String input) {
        if (this instanceof Searcher)
            ((Searcher) this).initSearch(input);
        else
            us.morphTo(Searcher.class, user).initSearch(input);
    }

    public final void reset() {
        entryId = user.rootId;

        if (!(this instanceof DirViewer)) {
            final DirViewer dw = us.morphTo(DirViewer.class, user);
            dw.scopeChanged();
            dw.doView();
        } else {
            ((DirViewer) this).scopeChanged();
            doView();
        }
    }

    public void joinShare(final String linkId) {
        final Share share = tfs.getPublicShare(linkId);

        if (share == null)
            doView();
        else {
            entryId = tfs.applyShareByLink(share, user).getId();

            if (!(this instanceof DirViewer)) {
                final DirViewer dw = us.morphTo(DirViewer.class, user);
                dw.scopeChanged();
                dw.doView();
            } else {
                ((DirViewer) this).scopeChanged();
                doView();
            }
        }
    }
}
