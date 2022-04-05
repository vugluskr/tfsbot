package model.request;

import com.fasterxml.jackson.databind.JsonNode;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 15:24
 * tfs ☭ sweat and blood
 */
public class TextRequest extends TgRequest {
    private final String text;

    public TextRequest(final JsonNode node, final String text0) {
        super(node.get("from"));
        this.text = notNull(text0);
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean isCrooked() {
        return isEmpty(text);
    }
}
