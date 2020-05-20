package model;


import utils.Strings;

import java.util.Objects;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class User {
    private long id;
    private String lang;
    private long lastMessageId;
    private long lastDialogId;
    private String state,
            fallback,
            query;
    private long dirId;
    private int offset,
            searchOffset,
            searchCount;

    private volatile boolean changed;

    public long getId() {
        return id;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(final String fallback) {
        if (Objects.equals(fallback, this.fallback))
            return;

        changed = true;
        this.fallback = fallback;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(final long lastMessageId) {
        if (Objects.equals(lastMessageId, this.lastMessageId))
            return;

        changed = true;

        this.lastMessageId = lastMessageId;
    }

    public long getLastDialogId() {
        return lastDialogId;
    }

    public void setLastDialogId(final long lastDialogId) {
        if (Objects.equals(lastDialogId, this.lastDialogId))
            return;

        changed = true;
        this.lastDialogId = lastDialogId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(final String lang) {
        this.lang = lang;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        if (Objects.equals(state, this.state))
            return;

        changed = true;
        this.state = state;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        if (Objects.equals(query, this.query))
            return;

        changed = true;
        this.query = query;
    }

    public long getDirId() {
        return dirId;
    }

    public void setDirId(final long dirId) {
        if (Objects.equals(dirId, this.dirId))
            return;

        changed = true;
        this.dirId = dirId;
    }

    public int getOffset() {
        return offset;
    }

    public void deltaOffset(final int delta) {
        offset = Math.max(0, offset + delta);
        if (!changed && delta != 0)
            changed = true;
    }

    public void setOffset(final int offset) {
        if (Objects.equals(offset, this.offset))
            return;

        changed = true;
        this.offset = offset;
    }

    public int getSearchOffset() {
        return searchOffset;
    }

    public void setSearchOffset(final int searchOffset) {
        if (Objects.equals(searchOffset, this.searchOffset))
            return;

        changed = true;

        this.searchOffset = searchOffset;
    }

    public int getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(final int searchCount) {
        if (Objects.equals(searchCount, this.searchCount))
            return;

        changed = true;
        this.searchCount = searchCount;
    }

    public void deltaSearchOffset(final int delta) {
        searchOffset = Math.max(0, searchOffset + delta);

        if (!changed && delta != 0)
            changed = true;
    }

    public void switchBack() {
        setState(fallback);
        setFallback(Strings.State.View);
    }

    public boolean isChanged() {
        return changed;
    }
}
