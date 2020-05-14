package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditCaption {
//    chat_id 	Integer or String 	Optional 	Required if inline_message_id is not specified. Unique identifier for the target chat or username of the target channel (in the format @channelusername)
//    message_id 	Integer 	Optional 	Required if inline_message_id is not specified. Identifier of the message to edit
//    inline_message_id 	String 	Optional 	Required if chat_id and message_id are not specified. Identifier of the inline message
//    caption 	String 	Optional 	New caption of the message, 0-1024 characters after entities parsing
//    parse_mode 	String 	Optional 	Mode for parsing entities in the message caption. See formatting options for more details.
//    reply_markup 	InlineKeyboardMarkup 	Optional 	A JSON-serialized object for an inline keyboard.
    @JsonProperty("chat_id")
    private long chatId;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("inline_message_id")
    private long inlineMessageId;
    private String caption;
    @JsonProperty("parse_mode")
    private String parseMode;
    @JsonProperty("reply_markup")
    private ReplyMarkup replyMarkup;

    public EditCaption() {
    }

    public EditCaption(final long chatId, final long messageId, final String caption) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.caption = caption;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(final long chatId) {
        this.chatId = chatId;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(final long messageId) {
        this.messageId = messageId;
    }

    public long getInlineMessageId() {
        return inlineMessageId;
    }

    public void setInlineMessageId(final long inlineMessageId) {
        this.inlineMessageId = inlineMessageId;
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

    public ReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(final ReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }
}
