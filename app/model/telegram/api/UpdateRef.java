package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 17:20
 * SIRBot ☭ sweat and blood
 */
public class UpdateRef {
    @JsonProperty("update_id")
    private long id;

    private MessageRef message;

    @JsonProperty("edited_message")
    private MessageRef editedMessage;

    @JsonProperty("callback_query")
    private CallbackRef callback;

    public CallbackRef getCallback() {
        return callback;
    }

    public void setCallback(final CallbackRef callback) {
        this.callback = callback;
    }

    public MessageRef getEditedMessage() {
        return editedMessage;
    }

    public void setEditedMessage(final MessageRef editedMessage) {
        this.editedMessage = editedMessage;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public MessageRef getMessage() {
        return message;
    }

    public void setMessage(final MessageRef message) {
        this.message = message;
    }
}
