package utils;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public enum  UOpts {
    Gui, Russian;

    public final int bitmask() {
        return 1 << ordinal();
    }

    public final void reverse(final User u) {
        if (is(u))
            clear(u);
        else
            set(u);
    }

    public final boolean is(final User u) {
        return (u.getOptions() & bitmask()) > 0;
    }

    public final void set(final User user) {
        user.setOptions(user.getOptions() | bitmask());
    }

    public final void clear(final User user) {
        user.setOptions(user.getOptions() & ~bitmask());
    }
}
