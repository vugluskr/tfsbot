package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.10.2017 07:30
 * SIRBot ☭ sweat and blood
 */
public class VoiceRef implements TeleFile {
    private int duration;
    @JsonProperty("mime_type")
    private String mimeType;

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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(final String fileId) {
        this.fileId = fileId;
    }

    @Override
    public ContentType getType() {
        return ContentType.VOICE;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }
}
