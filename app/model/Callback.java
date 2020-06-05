package model;

import utils.Strings;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.05.2020
 * tfs ☭ sweat and blood
 */
public class Callback {
    public final Strings.Callback type;
    public final int idx;

    public Callback(final Strings.Callback type, final int idx) {
        this.type = type;
        this.idx = idx;
    }

    public Strings.Callback type() {
        return type;
    }
}
