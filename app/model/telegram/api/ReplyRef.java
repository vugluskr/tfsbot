package model.telegram.api;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 17:23
 * SIRBot ☭ sweat and blood
 */
public class ReplyRef {
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
