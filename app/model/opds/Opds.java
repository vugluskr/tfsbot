package model.opds;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 13:27
 * tfs ☭ sweat and blood
 */
public class Opds {
    private int id;
    private String title;

    private String url;

    private LocalDateTime updated;

    public final List<Folder> childs = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Opds{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", updated=" + updated +
                ", childs=" + childs +
                '}';
    }
}
