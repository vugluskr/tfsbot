package model.opds;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:02
 * tfs ☭ sweat and blood
 */
public class OpdsBook {
    private String title;
    private SortedSet<String> authors;
    private int year;
    private String id;
    private String fbLink;
    private String epubLink;
    private String fbRefId;
    private String epubRefId;
    private Set<String> genres;

    {
        authors = new TreeSet<>();
        genres = new HashSet<>(0);
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void setGenres(final Set<String> genres) {
        this.genres = genres;
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

    @Override
    public String toString() {
        return "OpdsBook{" +
                "title='" + title + '\'' +
                ", authors=" + authors +
                ", year=" + year +
                ", id='" + id + '\'' +
                ", fbLink='" + fbLink + '\'' +
                ", epubLink='" + epubLink + '\'' +
                ", fbRefId='" + fbRefId + '\'' +
                ", epubRefId='" + epubRefId + '\'' +
                ", genres=" + genres +
                '}';
    }
}
