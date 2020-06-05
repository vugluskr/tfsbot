package utils;

import services.GUI;
import services.TgApi;

/**
 * @author Denis Danilin | denis@danilin.name
 * 30.05.2020
 * tfs ☭ sweat and blood
 */
public class FlowBox {
    public String format = null;
    public final StringBuilder body = new StringBuilder(16);
    public final GUI.Keyboard kbd = new GUI.Keyboard();

    public FlowBox md2() {
        format = TgApi.formatMd2;

        return this;
    }

    public FlowBox body(final String part) {
        body.append(part);

        return this;
    }

    public FlowBox row() {
        kbd.newLine();

        return this;
    }

    public FlowBox button(final GUI.Button button) {
        kbd.button(button);

        return this;
    }

    public FlowBox setListing(final boolean hasLess, final boolean hasMore) {
        kbd.newLine();
        if (hasLess)
            kbd.button(GUI.Buttons.rewindButton);
        if (hasMore)
            kbd.button(GUI.Buttons.forwardButton);

        return this;
    }
}
