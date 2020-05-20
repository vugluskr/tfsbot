package model;


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

    public long getId() {
        return id;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(final String fallback) {
        this.fallback = fallback;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(final long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public long getLastDialogId() {
        return lastDialogId;
    }

    public void setLastDialogId(final long lastDialogId) {
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
        this.state = state;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public long getDirId() {
        return dirId;
    }

    public void setDirId(final long dirId) {
        this.dirId = dirId;
    }

    public int getOffset() {
        return offset;
    }

    public void deltaOffset(final int delta) {
        offset = Math.max(0, offset + delta);
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public int getSearchOffset() {
        return searchOffset;
    }

    public void setSearchOffset(final int searchOffset) {
        this.searchOffset = searchOffset;
    }

    public int getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(final int searchCount) {
        this.searchCount = searchCount;
    }

    public void deltaSearchOffset(final int delta) {
        searchOffset = Math.max(0, searchOffset + delta);
    }
}
