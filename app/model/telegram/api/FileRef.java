package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class FileRef {
//    file_id 	String 	Identifier for this file, which can be used to download or reuse the file
    @JsonProperty("file_id")
    private String id;

//    file_unique_id 	String 	Unique identifier for this file, which is supposed to be the same over time and for different bots. Can't be used to download or reuse the file.
    @JsonProperty("file_unique_id")
    private String fileUniqueId;

//    file_size 	Integer 	Optional. File size, if known
    @JsonProperty("file_size")
    private long fileSize;

//    file_path 	String 	Optional. File path. Use https://api.telegram.org/file/bot<token>/<file_path> to get the file.
    @JsonProperty("file_path")
    private String filePath;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getFileUniqueId() {
        return fileUniqueId;
    }

    public void setFileUniqueId(final String fileUniqueId) {
        this.fileUniqueId = fileUniqueId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }
}
