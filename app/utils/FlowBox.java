package utils;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import services.GUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 30.05.2020
 * tfs ☭ sweat and blood
 */
public class FlowBox {
    public String format = null;
    public final StringBuilder body = new StringBuilder(16);
    public final List<List<InlineButton>> rows = new ArrayList<>(0);

    public FlowBox md2() {
        format = "MarkdownV2";

        return this;
    }

    public FlowBox body(final String part) {
        body.append(part);

        return this;
    }

    public FlowBox row() {
        rows.add(new ArrayList<>(0));

        return this;
    }

    public FlowBox button(final InlineButton button) {
        if (rows.isEmpty())
            row();

        rows.get(rows.size() - 1).add(button);

        return this;
    }

    public FlowBox setListing(final boolean hasLess, final boolean hasMore) {
        row();
        if (hasLess)
            button(GUI.Buttons.rewindButton);
        if (hasMore)
            button(GUI.Buttons.forwardButton);

        return this;
    }
}
