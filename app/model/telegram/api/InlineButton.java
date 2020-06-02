package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.Callback;
import utils.Strings;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.01.2018 16:10
 * SIRBot ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineButton {
    private String text;
    @JsonProperty("callback_data")
    private String callbackData;

    @JsonProperty("switch_inline_query_current_chat")
    private String inlineQuery;

    public InlineButton() {
    }

    public InlineButton(final String text, final Strings.Callback callbackData) {
        this(text, callbackData.toString());
    }

    public InlineButton(final String text, final String callbackData) {
        this.text = text;
        this.callbackData = callbackData;
    }

    public String getInlineQuery() {
        return inlineQuery;
    }

    public void setInlineQuery(final String inlineQuery) {
        this.inlineQuery = inlineQuery;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(final String callbackData) {
        this.callbackData = callbackData;
    }
}
