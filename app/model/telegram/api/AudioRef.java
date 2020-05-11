package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

import java.util.regex.Pattern;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.10.2017 07:33
 * SIRBot ☭ sweat and blood
 */
public class AudioRef implements TeleFile {
    @JsonProperty("mime_type")
    private String mimeType;
    private String title;
    private String performer;

    private int duration;

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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(final String performer) {
        this.performer = performer;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
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
        return ContentType.AUDIO;
    }

    @Override
    public String getFileName() {
        final StringBuilder s = new StringBuilder(16);
        s.append(notNull(title)).append("_").append(notNull(performer));

        if (s.toString().trim().length() == 1)
            return null;

        return s.toString()
                .trim()
                .replaceAll(Pattern.quote("/"), ".")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("\\s", ".")
                .replaceAll(Pattern.quote("-"), "_")
                + ContentType.AUDIO.ext;
    }
}
