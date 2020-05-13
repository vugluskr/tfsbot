package utils;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public enum UserMode {
    None, MkDirWaitName,;

    public static UserMode resolve(final User user) {
        if (user.getMode() > 0)
            try { return UserMode.values()[user.getMode()]; } catch (final Exception ignore) { }

        return None;
    }

    public boolean is(final User user) {
        return user.getMode() == ordinal();
    }
}
