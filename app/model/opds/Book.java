package model.opds;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:02
 * tfs ☭ sweat and blood
 */
public class Book {
    private String title;
    private String content;
    private SortedSet<String> authors;
    private int year;
    private String id;
    private String fbLink;
    private String epubLink;
    private String fbRefId;
    private String epubRefId;

    {
        authors = new TreeSet<>();
    }

    public SortedSet<String> getAuthors() {
        return authors;
    }

    public void setAuthors(final SortedSet<String> authors) {
        this.authors = authors;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public int getYear() {
        return year;
    }

    public void setYear(final int year) {
        this.year = year;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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
}
