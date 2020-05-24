package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextRef {
    private String text;

    @JsonProperty("chat_id")
    private long chatId;

    @JsonProperty("parse_mode")
    private String parseMode;

    @JsonProperty("disable_web_page_preview")
    private boolean disablePreview;

    @JsonProperty("reply_markup")
    private ReplyMarkup replyMarkup;

    public TextRef() {
        disablePreview = true;
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

    public boolean isDisablePreview() {
        return disablePreview;
    }

    public void setDisablePreview(final boolean disablePreview) {
        this.disablePreview = disablePreview;
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

    public void headRow(final InlineButton button) {
        if (replyMarkup == null || !(replyMarkup instanceof InlineKeyboard)) {
            replyMarkup = new InlineKeyboard();
            ((InlineKeyboard) replyMarkup).setKeyboard(new ArrayList<>());
            ((InlineKeyboard) replyMarkup).getKeyboard().add(new ArrayList<>());
        }

        ((InlineKeyboard) replyMarkup).getKeyboard().get(0).add(button);
    }

    public void row(final List<InlineButton> row) {
        if (replyMarkup == null || !(replyMarkup instanceof InlineKeyboard)) {
            replyMarkup = new InlineKeyboard();
            ((InlineKeyboard) replyMarkup).setKeyboard(new ArrayList<>());
        }

        ((InlineKeyboard) replyMarkup).getKeyboard().add(row);
    }

    public void row(final InlineButton... buttons) {
        if (isEmpty(buttons))
            return;

        row(Arrays.stream(buttons).collect(Collectors.toList()));
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
