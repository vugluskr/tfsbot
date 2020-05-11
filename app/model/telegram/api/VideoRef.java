package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:39
 * SIRBot ☭ sweat and blood
 */
public class VideoRef implements TeleFile {
    private int duration;
    private int width;
    private int height;
    @JsonProperty("mime_type")
    private String mimeType;
    private PhotoRef thumb;
    @JsonProperty("file_id")
    private String fileId;
    @JsonProperty("file_size")
    private long fileSize;
    @JsonProperty("file_unique_id")
    private String uniqId;

    public String getUniqId() {
        return uniqId;
    }

    public void setUniqId(final String uniqId) {
        this.uniqId = uniqId;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public PhotoRef getThumb() {
        return thumb;
    }

    public void setThumb(final PhotoRef thumb) {
        this.thumb = thumb;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(final String fileId) {
        this.fileId = fileId;
    }

    @Override
    public ContentType getType() {
        return ContentType.VIDEO;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }
}
