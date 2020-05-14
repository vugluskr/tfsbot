package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputMedia {
//    type 	String 	Type of the result, must be photo
//    media 	String 	File to send. Pass a file_id to send a file that exists on the Telegram servers (recommended), pass an HTTP URL for Telegram to get a file from the Internet, or pass “attach://<file_attach_name>” to upload a new one using multipart/form-data under <file_attach_name> name. More info on Sending Files »
//    caption 	String 	Optional. Caption of the photo to be sent, 0-1024 characters after entities parsing
//    parse_mode 	String 	Optional. Mode for parsing entities in the photo caption. See formatting options for more details.

    private String type;
    @JsonProperty("media")
    private String fileRefId;
    private String caption;
    @JsonProperty("parse_mdoe")
    private String parseMode;

    public InputMedia() {
    }

    public InputMedia(final String type, final String fileRefId, final String caption) {
        this.type = type;
        this.fileRefId = fileRefId;
        this.caption = caption;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getFileRefId() {
        return fileRefId;
    }

    public void setFileRefId(final String fileRefId) {
        this.fileRefId = fileRefId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
    }

    public String getParseMode() {
        return parseMode;
    }

    public void setParseMode(final String parseMode) {
        this.parseMode = parseMode;
    }
}
