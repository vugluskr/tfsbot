package model;

import utils.BMasked;
import utils.Optioned;

import java.util.Collection;
import java.util.TreeSet;
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
    private UUID currentDirId;
    private String query;
    private int viewOffset;
    private int options;

    public TreeSet<UUID> selection = new TreeSet<>();

    // not saved
    public String lang;
    public String name;

    @Override
    public String toString() {
        return "User{" +
                "lastMessageId=" + lastMessageId +
                ", currentDirId=" + currentDirId +
                ", query='" + query + '\'' +
                ", viewOffset=" + viewOffset +
                ", selection=" + selection +
                ", options=\n"+Optz.printOut(this)+"}";
    }

    public boolean isOnTop() {
        return currentDirId.equals(rootId);
    }

    public void deltaSearchOffset(final int delta) {
        viewOffset = Math.max(0, viewOffset + delta);
    }

    // getters/setters

    public TreeSet<UUID> getSelection() {
        return selection;
    }

    public void setSelection(final Collection<UUID> selection) {
        this.selection = selection == null ? new TreeSet<>() : new TreeSet<>(selection);
    }

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

    public UUID getCurrentDirId() {
        return currentDirId;
    }

    public void setCurrentDirId(final UUID currentDirId) {
        this.currentDirId = currentDirId;
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

    enum Optz implements BMasked {
        RenameInputWait, DirInputWait, LabelInputWait, GrantFileWait, StateSharing, StateMoving, StateGearing, StateSearching, SearchInputWait, StateFileViewing;

        public static String printOut(final User user) {
            final StringBuilder s = new StringBuilder();

            for (final Optz o : values())
                if (o.is(user))
                    s.append(o.name()).append(", ");

            return s.toString();
        }
    }
}
