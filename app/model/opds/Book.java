package model.opds;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:02
 * tfs ☭ sweat and blood
 */
public class Book {
    private long id;
    private String title;
    private String author;
    private int year;
    private String opdsTag;
    private String fbLink;
    private String epubLink;
    private String fbRefId;
    private String epubRefId;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
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

    public String getOpdsTag() {
        return opdsTag;
    }

    public void setOpdsTag(final String opdsTag) {
        this.opdsTag = opdsTag;
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

    public String getFbRefId() {
        return fbRefId;
    }

    public void setFbRefId(final String fbRefId) {
        this.fbRefId = fbRefId;
    }

    public String getEpubRefId() {
        return epubRefId;
    }

    public void setEpubRefId(final String epubRefId) {
        this.epubRefId = epubRefId;
    }

    public void addAuthor(final String author) {
        if (this.author == null)
            this.author = "";

        this.author += (isEmpty(this.author) ? "" : ", ") + author;
    }
}
