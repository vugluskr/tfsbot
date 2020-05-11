package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:44
 * SIRBot ☭ sweat and blood
 */
public class StickerRef implements TeleFile {
    private int width;
    private int height;
    private String emoji;
    @JsonProperty("set_name")
    private String setName;

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

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(final String emoji) {
        this.emoji = emoji;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(final String setName) {
        this.setName = setName;
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
        return ContentType.STICKER;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }
}
