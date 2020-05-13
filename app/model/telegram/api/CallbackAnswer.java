package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallbackAnswer {
//    callback_query_id 	String 	Yes 	Unique identifier for the query to be answered
//    text 	String 	Optional 	Text of the notification. If not specified, nothing will be shown to the user, 0-200 characters
//    show_alert 	Boolean 	Optional 	If true, an alert will be shown by the client instead of a notification at the top of the chat screen. Defaults to false.
//    url 	String 	Optional 	URL that will be opened by the user's client. If you have created a Game and accepted the conditions via @Botfather, specify the URL that opens your game — note that this will only work if the query comes from a callback_game button.
//    Otherwise, you may use links like t.me/your_bot?start=XXXX that open your bot with a parameter.
//    cache_time 	Integer 	Optional 	The maximum amount of time in seconds that the result of the callback query may be cached client-side. Telegram apps will support caching starting in version 3.14. Defaults to 0.

    @JsonProperty("callback_query_id")
    private long callbackId;
    private String text;
    @JsonProperty("show_alert")
    private boolean alert;
    private String url;
    @JsonProperty("cache_time")
    private int cacheTimeSeconds;

    public CallbackAnswer() {
    }

    public CallbackAnswer(final long callbackId, final String text) {
        this.callbackId = callbackId;
        this.text = text;
    }

    public CallbackAnswer(final long callbackId, final String text, final boolean alert) {
        this.callbackId = callbackId;
        this.text = text;
        this.alert = alert;
    }

    public long getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(final long callbackId) {
        this.callbackId = callbackId;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(final boolean alert) {
        this.alert = alert;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public int getCacheTimeSeconds() {
        return cacheTimeSeconds;
    }

    public void setCacheTimeSeconds(final int cacheTimeSeconds) {
        this.cacheTimeSeconds = cacheTimeSeconds;
    }
}
