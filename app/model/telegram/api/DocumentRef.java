package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.10.2017 06:35
 * SIRBot ☭ sweat and blood
 */
public class DocumentRef implements TeleFile {
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("mime_type")
    private String mimeType;
    private PhotoRef thumb;
    @JsonProperty("file_id")
    private String fileId;
    @JsonProperty("file_size")
    private long fileSize;

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getUniqId() {
        return null;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public ContentType getType() {
        return ContentType.DOCUMENT;
    }
}
