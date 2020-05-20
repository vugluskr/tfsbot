package actors.protocol;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class GotInput {
    public final String input;
    public final User user;

    public GotInput(final String input, final User user) {
        this.input = input;
        this.user = user;
    }
}
