package utils;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public enum  UOpts {
    Gui, WaitFolderName, MovingFile, GearMode, WaitFileName;

    public final int bitmask() {
        return 1 << ordinal();
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
