package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.ARole;
import model.user.CallbackSink;
import model.user.InputSink;
import model.user.Role;
import utils.LangMap;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public final class User {
    public final long id;
    public final UUID rootId;
    public final String lang;
    public final String name;
    public String lastRefId, lastText, lastKeyboard;

    public long lastMessageId;

    private Role role;

    public User(final long id,
                final UUID rootId,
                final String lang,
                final String name,
                final String lastRefId,
                final String lastText,
                final String lastKeyboard,
                final long lastMessageId,
                final Role role) {
        this.id = id;

        this.rootId = rootId;
        this.lang = lang;
        this.name = name;
        this.lastRefId = lastRefId;
        this.lastText = lastText;
        this.lastKeyboard = lastKeyboard;
        this.lastMessageId = lastMessageId;
        setRole(role);
    }

    public void setRole(final Role role) {
        this.role = role;
        ((ARole) this.role).user = this;
    }

    public final void doView() {
        role.doView();
    }

    public final LangMap.Value doHelp() {
        return role.helpValue();
    }

    public final JsonNode dump() {
        final ObjectNode data = (ObjectNode) role.dump();
        data.put("_class", role.getClass().getName());

        return data;
    }

    public void joinShare(final String id) {
        ((ARole) role).joinShare(id);
    }

    public void doSearch(final String text) {
        ((ARole) role).doSearch(text);
    }

    public void onFile(final TFile file) {
        ((ARole) role).onFile(file);
    }

    public void onCallback(final Command command) {
        if (role instanceof CallbackSink)
            ((CallbackSink) role).onCallback(command);
        else
            doView();
    }

    public void onInput(final String text) {
        if (role instanceof InputSink)
            ((InputSink) role).onInput(text);
        else
            doSearch(text);
    }

    public UUID entryId() {
        return ((ARole) role).entryId;
    }
}
