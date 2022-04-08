package model;

import services.BotApi;
import utils.AsButton;
import utils.BMasked;
import utils.Optioned;
import utils.Strings;

import java.nio.file.Paths;
import java.util.UUID;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class TFile implements Comparable<TFile>, Optioned, AsButton {
    public String uniqId;
    private UUID id, parentId;
    private long owner;
    public String refId;
    public ContentType type;
    private String name;
    private String path;
    private int options;
    private boolean rw;

    private boolean bookStore;

    @Override
    public BotApi.Button toButton(final int idx) {
        return new BotApi.Button(
                (isDir() ? (isBookStore() ? Strings.Uni.bookStore : Strings.Uni.folder) + " " : "")
                        + name,
                (isDir()
                        ? CommandType.openDir
                        : isFile()
                        ? CommandType.openFile
                        : CommandType.openLabel).toString()
                        + idx
        );
    }

    public String parentPath() {
        return Paths.get(path).getParent().toString();
    }

    public boolean isDir() {
        return type == ContentType.DIR;
    }

    public boolean isFile() {
        return type != ContentType.DIR && type != ContentType.LABEL;
    }

    @Override
    public int compareTo(final TFile o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TFile file = (TFile) o;

        return id.equals(file.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    //    getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(final UUID parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return (bookStore ? Strings.Uni.bookStore + " " : "") + name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(final String refId) {
        this.refId = refId;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(final ContentType type) {
        this.type = type;
    }

    public String getPath() {
        return notNull(path, "/");
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public boolean isLabel() {
        return type == ContentType.LABEL;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(final int options) {
        this.options = options;
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(final long owner) {
        this.owner = owner;
    }

    public boolean isRw() {
        return rw;
    }

    public void setRw(final boolean rw) {
        this.rw = rw;
    }

    public void setLocked() {
        Optz.locked.set(this);
    }

    public boolean isSharesRoot() {
        return Optz.sharesRoot.is(this);
    }

    public void setSharesRoot() {
        Optz.sharesRoot.set(this);
    }

    public void setShareFor(final long shareOwner) {
        Optz.shareFor.set(this);
        refId = String.valueOf(shareOwner);
    }

    public boolean isShareFor() {
        return Optz.shareFor.is(this);
    }

    public boolean isLocked() {
        return Optz.locked.is(this);
    }

    public void setUnlocked() {
        Optz.locked.remove(this);
    }

    public boolean isBookStore() {
        return bookStore;
    }

    public void setBookStore(final boolean bookStore) {
        this.bookStore = bookStore;
    }

    public void setGenres() {
        Optz.Genres.set(this);
    }

    public void setAuthors() {
        Optz.Authors.set(this);
    }

    public void setAbc() {
        Optz.Abc.set(this);
    }

    public boolean isAbc() {
        return Optz.Abc.is(this);
    }

    public boolean isAuthors() {
        return Optz.Authors.is(this);
    }

    public boolean isGenres() {
        return Optz.Genres.is(this);
    }

    public boolean isBookDir() {
        return isAbc() || isAuthors() || isGenres();
    }

    enum Optz implements BMasked {
        unused, locked, sharesRoot, Genres, Authors, Abc, shareFor
    }
}
