package actors.protocol;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class GotCallback {
    public long callbackId;
    public final String callbackData;
    public final User user;

    public GotCallback(final long callbackId, final String callbackData, final User user) {
        this.callbackId = callbackId;
        this.callbackData = callbackData;
        this.user = user;
    }
}
