package model;

import services.TgApi;
import utils.BMasked;
import utils.Optioned;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class User implements Optioned, InputWait, Stater {
    // not changed never ever
    private long id;
    private UUID rootId;

    // runtime data
    private long lastMessageId;
    private UUID subjectId, searchDirId;
    private String query, lastRefId, lastText, lastKeyboard;
    private int viewOffset;
    private int options;

    // not saved
    public String lang;
    public String name;

    @Override
    public String toString() {
        return "User{" +
                "lastMessageId=" + lastMessageId +
                ", subjectId=" + subjectId +
                ", query='" + query + '\'' +
                ", viewOffset=" + viewOffset +
                ", options=\n"+Optz.printOut(this)+"}";
    }

    public boolean isOnTop() {
        return subjectId.equals(rootId);
    }

    public void deltaSearchOffset(final int delta) {
        viewOffset = Math.max(0, viewOffset + delta);
    }

    // getters/setters

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(final String lang) {
        this.lang = lang;
    }

    public UUID getRootId() {
        return rootId;
    }

    public void setRootId(final UUID rootId) {
        this.rootId = rootId;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(final long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(final UUID subjectId) {
        this.subjectId = subjectId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public int getViewOffset() {
        return viewOffset;
    }

    public void setViewOffset(final int viewOffset) {
        this.viewOffset = viewOffset;
    }

    public UUID getSearchDirId() {
        return searchDirId;
    }

    public void setSearchDirId(final UUID searchDirId) {
        this.searchDirId = searchDirId;
    }

    @Override
    public int getOptions() {
        return options;
    }

    @Override
    public void setOptions(final int options) {
        this.options = options;
    }

    public void setWaitFileGranting() { Optz.GrantFileWait.set(this); }
    public void unsetWaitFileGranting() { Optz.GrantFileWait.remove(this); }
    public boolean isWaitFileGranting() { return Optz.GrantFileWait.is(this); }

    public void cancelWaiting() {
        Optz.GrantFileWait.remove(this);
        resetInputWait();
    }

    public String getLastRefId() {
        return lastRefId;
    }

    public void setLastRefId(final String lastRefId) {
        this.lastRefId = lastRefId;
    }

    public String getLastText() {
        return lastText;
    }

    public void setLastText(final String lastText) {
        this.lastText = lastText;
    }

    public void setLastKeyboard(final String lastKeyboard) {
        this.lastKeyboard = lastKeyboard;
    }

    public TgApi.Keyboard getLastKeyboard() {
        return lastKeyboard == null ? null : TgApi.Keyboard.fromJson(lastKeyboard);
    }

    public enum Optz implements BMasked {
        RenameDirInputWait,
        RenameFileInputWait,
        EditLabelInputWait,
        DirInputWait,
        LabelInputWait,
        GrantFileWait,
        StateSearching,
        StateSharing, StateGearing, PasswordInputWait, UnlockDirInputWait, UnlockFileInputWait;

        public static String printOut(final User user) {
            final StringBuilder s = new StringBuilder();

            for (final Optz o : values())
                if (o.is(user))
                    s.append(o.name()).append(", ");

            return s.toString();
        }
    }
}
