package model;

import model.telegram.ContentType;
import utils.BMasked;
import utils.Optioned;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class TFile implements Comparable<TFile>, Optioned {
    private long id;
    private long parentId;
    private String refId;
    private ContentType type;
    private String name, path;
    private boolean selected, found;
    private int options;

    public boolean isDir() {
        return type == ContentType.DIR;
    }

    @Override
    public int compareTo(final TFile o) {
        final int res = Long.compare(parentId, o.parentId);
        return res != 0 ? res : name.compareTo(o.name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TFile file = (TFile) o;

        if (parentId != file.parentId) return false;
        return name.equals(file.name);
    }

    @Override
    public int hashCode() {
        int result = (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + name.hashCode();
        return result;
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
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(final long parentId) {
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(final boolean found) {
        this.found = found;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(final int options) {
        this.options = options;
    }

    public boolean isShared() {
        return Optz.shared.is(options);
    }

    public void setUnshared() {
        Optz.shared.remove(this);
    }

    public void setShared() {
        Optz.shared.set(this);
    }

    enum Optz implements BMasked {
        shared, locked;
    }
}
