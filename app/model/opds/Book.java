package model.opds;

import java.time.LocalDateTime;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:02
 * tfs ☭ sweat and blood
 */
public class Book {
    private long id;
    private int opdsId;
    private long folderId;
    private LocalDateTime updated;
    private String title;
    private String author;
    private int year;
    private String desc;
    private String tag;
    private String fbLink;
    private String epubLink;
    private String refId;

    public String getRefId() {
        return refId;
    }

    public void setRefId(final String refId) {
        this.refId = refId;
    }

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

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(final long folderId) {
        this.folderId = folderId;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public int getYear() {
        return year;
    }

    public void setYear(final int year) {
        this.year = year;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    public String getFbLink() {
        return fbLink;
    }

    public void setFbLink(final String fbLink) {
        this.fbLink = fbLink;
    }

    public String getEpubLink() {
        return epubLink;
    }

    public void setEpubLink(final String epubLink) {
        this.epubLink = epubLink;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", opdsId=" + opdsId +
                ", folderId=" + folderId +
                ", updated=" + updated +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", year=" + year +
                ", desc='" + desc + '\'' +
                ", tag='" + tag + '\'' +
                ", fbLink='" + fbLink + '\'' +
                ", epubLink='" + epubLink + '\'' +
                '}';
    }

    public void addAuthor(final String author) {
        this.author += (isEmpty(this.author) ? "" : ", ") + author;
    }
}
