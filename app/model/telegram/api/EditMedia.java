package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditMedia {
//    chat_id 	Integer or String 	Optional 	Required if inline_message_id is not specified. Unique identifier for the target chat or username of the target channel (in the format @channelusername)
//    message_id 	Integer 	Optional 	Required if inline_message_id is not specified. Identifier of the message to edit
//    inline_message_id 	String 	Optional 	Required if chat_id and message_id are not specified. Identifier of the inline message
//    media 	InputMedia 	Yes 	A JSON-serialized object for a new media content of the message
//    reply_markup 	InlineKeyboardMarkup 	Optional 	A JSON-serialized object for a new inline keyboard.

    @JsonProperty("chat_id")
    private long chatId;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("inline_message_id")
    private String inlineMessageId;
    @JsonProperty("media")
    private String fileRefId;
    @JsonProperty("reply_markup")
    private ReplyMarkup replyMarkup;

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

    public String getInlineMessageId() {
        return inlineMessageId;
    }

    public void setInlineMessageId(final String inlineMessageId) {
        this.inlineMessageId = inlineMessageId;
    }

    public String getFileRefId() {
        return fileRefId;
    }

    public void setFileRefId(final String fileRefId) {
        this.fileRefId = fileRefId;
    }

    public ReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(final ReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }
}
