package model.opds;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 13:30
 * tfs ☭ sweat and blood
 */
public class Folder {
    private long id;
    private long parentId;
    private int opdsId;
    private String title;
    private String desc;
    private String path;
    private LocalDateTime updated;
    private String tag;

    public final List<Folder> childs = new ArrayList<>();
    public final List<Book> books = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public int getOpdsId() {
        return opdsId;
    }

    public void setOpdsId(final int opdsId) {
        this.opdsId = opdsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(final long parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id=" + id +
                ", opdsId=" + opdsId +
                ", parentId=" + parentId +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", path='" + path + '\'' +
                ", updated=" + updated +
                ", tag='" + tag + '\'' +
                ", childs=" + childs +
                ", books=" + books +
                '}';
    }

    public void mergeChilds(final Collection<Folder> folders) {
        OUTER:
        for (final Folder fresh : folders) {
            for (final Folder exist : childs)
                if (exist.getTag().equals(fresh.getTag())) {
                    exist.setUpdated(fresh.updated);
                    exist.setDesc(fresh.desc);
                    exist.setTitle(fresh.title);
                    exist.setPath(fresh.path);
                    continue OUTER;
                }

            fresh.setParentId(id);
            fresh.setOpdsId(opdsId);
            childs.add(fresh);
        }
    }

    public void mergeBooks(final Collection<Book> books) {
        OUTER:
        for (final Book fresh : books) {
            for (final Book exist : this.books)
                if (exist.getTag().equals(fresh.getTag())) {
                    exist.setYear(fresh.getYear());
                    exist.setTitle(fresh.getTitle());
                    exist.setDesc(fresh.getDesc());
                    exist.setFbLink(fresh.getFbLink());
                    exist.setEpubLink(fresh.getEpubLink());
                    exist.setAuthor(fresh.getAuthor());
                    continue OUTER;
                }

            fresh.setFolderId(id);
            fresh.setOpdsId(opdsId);
            this.books.add(fresh);
        }
    }
}
