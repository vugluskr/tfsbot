package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User {
    private long id;
    private String lang;
    private long lastMessageId;
    private long lastDialogId;
    private String state;
    private String fallback, query;
    private int offset,
            searchOffset;
    private String name;

    public TFile current, parent;
    public List<TFile> selection, searchResults;

    public List<Object> view;

    @JsonIgnore
    public volatile boolean skipTail;

    public User() {
        selection = new ArrayList<>();
        searchResults = new ArrayList<>();
        view = new ArrayList<>(0);
    }

    public List<Object> getView() {
        return view;
    }

    public void setView(final List<Object> view) {
        this.view = view;
    }

    public TFile getCurrent() {
        return current;
    }

    public void setCurrent(final TFile current) {
        this.current = current;
    }

    public TFile getParent() {
        return parent;
    }

    public void setParent(final TFile parent) {
        this.parent = parent;
    }

    public String getPath() {
        return notNull(current == null ? null : current.getPath(), "/");
    }

    public List<TFile> getSelection() {
        return selection;
    }

    public void setSelection(final List<TFile> selection) {
        this.selection = selection;
    }

    public List<TFile> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(final List<TFile> searchResults) {
        this.searchResults = searchResults;
    }

    public long getId() {
        return id;
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

    public void deltaSearchOffset(final int delta) {
        searchOffset = Math.max(0, searchOffset + delta);
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(final String fallback) {
        this.fallback = fallback;
    }

    public boolean hasMovable() {
        return !selection.isEmpty() && selection.stream().anyMatch(f -> f.getOwner() == id && f.isSharable());
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void newView() {
        view.clear();
    }

    public void viewAdd(final Object o) {
        if (o == null)
            return;

        view.add(o);
    }

    public int viewIdx() {
        return view.isEmpty() ? 0 : view.size() - 1;
    }
}
