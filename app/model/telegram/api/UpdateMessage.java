package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateMessage {
    @JsonProperty("chat_id")
    private long chatId;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("inline_message_id")
    private long inlineMessageId;
    @JsonProperty("text")
    private String text;
    @JsonProperty("parse_mode")
    private String parseMode;
    @JsonProperty("disable_web_page_preview")
    private boolean disablePreview;
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

    public long getInlineMessageId() {
        return inlineMessageId;
    }

    public void setInlineMessageId(final long inlineMessageId) {
        this.inlineMessageId = inlineMessageId;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getParseMode() {
        return parseMode;
    }

    public void setParseMode(final String parseMode) {
        this.parseMode = parseMode;
    }

    public boolean isDisablePreview() {
        return disablePreview;
    }

    public void setDisablePreview(final boolean disablePreview) {
        this.disablePreview = disablePreview;
    }

    public ReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(final ReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }
}
