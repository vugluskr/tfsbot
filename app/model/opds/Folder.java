package model.opds;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 13:30
 * tfs ☭ sweat and blood
 */
public class Folder {
    private long id;
    private String title;
    private String desc;
    private String path;
    private String tag;

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

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }
}
