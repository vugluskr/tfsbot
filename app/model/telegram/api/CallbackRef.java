package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 27.10.2017 16:43
 * SIRBot ☭ sweat and blood
 */
public class CallbackRef {
    private long id;
    @JsonProperty("chat_instance")
    private long chatInstance;
    private String data;
    private ContactRef from;
    private MessageRef message;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getChatInstance() {
        return chatInstance;
    }

    public void setChatInstance(final long chatInstance) {
        this.chatInstance = chatInstance;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public ContactRef getFrom() {
        return from;
    }

    public void setFrom(final ContactRef from) {
        this.from = from;
    }

    public MessageRef getMessage() {
        return message;
    }

    public void setMessage(final MessageRef message) {
        this.message = message;
    }
}
