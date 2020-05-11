package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:32
 * SIRBot ☭ sweat and blood
 */
public class PhotoRef implements TeleFile {
    @JsonProperty("file_id")
    private String fileId;

    @JsonProperty("file_size")
    private long fileSize;

    private int width, height;

    @JsonProperty("file_unique_id")
    private String uniqId;

    public String getUniqId() {
        return uniqId;
    }

    public void setUniqId(final String uniqId) {
        this.uniqId = uniqId;
    }
    public String getFileId() {
        return fileId;
    }

    public void setFileId(final String fileId) {
        this.fileId = fileId;
    }

    @Override
    public ContentType getType() {
        return ContentType.PHOTO;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }
}
