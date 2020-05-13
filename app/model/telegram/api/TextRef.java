package model.telegram.api;

import com.fasterxml.jackson.annotation.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class TextRef {
    private String text;

    @JsonProperty("chat_id")
    private long chatId;

    @JsonProperty("parse_mode")
    private String parseMode;

    @JsonProperty("reply_markup")
    private ReplyMarkup replyMarkup;

    public TextRef() {
    }

    public TextRef(final String text, final long chatId) {
        this.text = text;
        this.chatId = chatId;
    }

    public TextRef wrapMw() {
        this.text = "```\n" + text + "\n```";
        return setMd2();
    }

    public TextRef setPlain() {
        parseMode = null;
        return this;
    }

    public TextRef setMd2() {
        parseMode = "MarkdownV2";
        return this;
    }

    public TextRef setMd() {
        parseMode = "Markdown";
        return this;
    }

    public TextRef setHtml() {
        parseMode = "HTML";
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(final long chatId) {
        this.chatId = chatId;
    }

    public String getParseMode() {
        return parseMode;
    }

    public void setParseMode(final String parseMode) {
        this.parseMode = parseMode;
    }
    public TextRef withKeyboard(final InlineKeyboard keyboard) {
        setReplyMarkup(keyboard);
        return this;
    }

    public TextRef withForcedReply() {
        setReplyMarkup(new ForceReply());
        return this;
    }

    public ReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(final ReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }

    @Override
    public String toString() {
        return "TextRef{" +
                "text='" + text + '\'' +
                ", chatId=" + chatId +
                ", parseMode='" + parseMode + '\'' +
                '}';
    }
}
