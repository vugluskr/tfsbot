package model;

import model.telegram.ContentType;
import model.telegram.api.TeleFile;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class TFile implements Comparable<TFile> {
    private String refId;
    private long size;
    private ContentType type;
    private long id;
    private long parentId;
    private long indate;
    private String name, path;

    public volatile boolean exist;

    public static TFile mkdir(final String name) {
        return new TFile(ContentType.DIR, 0, null, null, name);
    }

    public static TFile stub(final TFile pro) {
        final TFile f = new TFile();
        f.indate = pro.indate;
        f.type = pro.type;
        f.id = pro.id;
        f.parentId = pro.parentId;
        f.path = pro.path;

        return f;
    }

    public TFile(final TeleFile teleFile) {
        this(teleFile.getType(), teleFile.getFileSize(), teleFile.getFileId(), teleFile.getUniqId(), teleFile.getFileName());
    }

    public TFile(final ContentType type, final long size, final String refId, final String uniqId, final String caption) {
        this.type = type;

        this.refId = refId;
        this.size = size;

        this.name = notNull(caption, type.name().toLowerCase() + "_" + uniqId + type.ext);
    }

    public TFile(final long parentId, final String name) {
        this.parentId = parentId;
        this.name = name;
        this.type = ContentType.DIR;
    }

    public TFile(final long id) {
        this.id = id;
    }

    public TFile() {
    }

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
                ", size=" + size +
                ", type=" + type +
                ", id=" + id +
                ", parentId=" + parentId +
                ", indate=" + indate +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", exist=" + exist +
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

    public long getSize() {
        return size;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(final ContentType type) {
        this.type = type;
    }

    public long getIndate() {
        return indate;
    }

    public void setIndate(final long indate) {
        this.indate = indate;
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
}
