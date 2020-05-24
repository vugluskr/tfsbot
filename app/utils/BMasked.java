package utils;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.05.2020
 * tfs ☭ sweat and blood
 */
public interface BMasked {
    int ordinal();

    default int bitmask() {
        return 1 << ordinal();
    }

    default int set(final int options) {
        int o = options;
        o |= bitmask();

        return o;
    }

    default int remove(final int options) {
        int o = options;

        o &= ~bitmask();

        return o;
    }

    default boolean is(final int options) {
        return (options & bitmask()) > 0;
    }

    default void set(final Optioned options) {
        options.setOptions(options.getOptions() | bitmask());
    }

    default void remove(final Optioned options) {
        options.setOptions(options.getOptions() & ~bitmask());
    }

    default boolean is(final Optioned options) {
        return (options.getOptions() & bitmask()) > 0;
    }
}
