package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 10.05.2020
 * pmatrix ☭ sweat and blood
 */
public class ForceReply implements ReplyMarkup {
    @JsonProperty("force_reply")
    private boolean forceReply;

    private boolean selective;

    public ForceReply() {
        this.forceReply = true;
    }

    public boolean isForceReply() {
        return forceReply;
    }

    public void setForceReply(final boolean forceReply) {
        this.forceReply = forceReply;
    }

    public boolean isSelective() {
        return selective;
    }

    public void setSelective(final boolean selective) {
        this.selective = selective;
    }
}
