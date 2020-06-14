package model;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.05.2020
 * tfs ☭ sweat and blood
 */
public class CallbackData {
    public final CommandType type;
    public final int idx;

    public CallbackData(final CommandType type, final int idx) {
        this.type = type;
        this.idx = idx;
    }

    public CommandType type() {
        return type;
    }
}
