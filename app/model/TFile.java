package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import model.telegram.ContentType;
import utils.BMasked;
import utils.Optioned;

import java.util.UUID;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TFile implements Comparable<TFile>, Optioned {
    private UUID id, parentId;
    private long owner;
    private String refId;
    private ContentType type;
    private String name, path;
    private int options;
    private boolean rw;

    public volatile boolean selected;

    public boolean isDir() {
        return type == ContentType.DIR;
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

    @Override
    public String toString() {
        return "TFile{" +
                "refId='" + refId + '\'' +
                ", type=" + type +
                ", id=" + id +
                ", parentId=" + parentId +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                '}';
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
        return name;
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

    @JsonIgnore
    public boolean isShared() {
        return Optz.shared.is(options);
    }

    @JsonIgnore
    public void setUnshared() {
        Optz.shared.remove(this);
    }

    @JsonIgnore
    public void setShared() {
        Optz.shared.set(this);
    }

    @JsonIgnore
    public void setLocked() {
        Optz.locked.set(this);
    }

    @JsonIgnore
    public boolean isSharable() {
        return !isSharesRoot() && !isShared() && !isShareFor() && rw;
    }

    @JsonIgnore
    public boolean isSharesRoot() {
        return Optz.sharesRoot.is(this);
    }

    @JsonIgnore
    public void setSharesRoot() {
        Optz.sharesRoot.set(this);
    }

    @JsonIgnore
    public void setShareFor(final long shareOwner) {
        Optz.shareFor.set(this);
        refId = String.valueOf(shareOwner);
    }

    @JsonIgnore
    public boolean isShareFor() {
        return Optz.shareFor.is(this);
    }

    enum Optz implements BMasked {
        shared, locked, sharesRoot, shareFor;
    }
}
