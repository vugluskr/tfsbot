package model.telegram.api;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public class ApiMessageReply {
    private boolean ok;
    private MessageRef result;

    public boolean isOk() {
        return ok;
    }

    public void setOk(final boolean ok) {
        this.ok = ok;
    }

    public MessageRef getResult() {
        return result;
    }

    public void setResult(final MessageRef result) {
        this.result = result;
    }
}
