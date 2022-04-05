package model.request;

import com.fasterxml.jackson.databind.JsonNode;
import model.ContentType;
import model.user.TgUser;

import static model.ContentType.media;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 14:56
 * tfs ☭ sweat and blood
 */
public abstract class TgRequest {
    public final TgUser user;

    protected TgRequest(final JsonNode node) {
        user = new TgUser(node);
    }

    public static TgRequest resolve(final JsonNode node) {
        TgRequest request = null;

        if (node.has("callback_query"))
            request = new CallbackRequest(node);
        else if (node.has("message")) {
            if (node.has("text")) {
                final String t = notNull(node.get("text").asText());

                if (t.startsWith("/"))
                    request = new CmdRequest(node, t);
                else
                    request = new TextRequest(node, t);
            } else
                for (final ContentType t : media)
                    if (node.has(t.getParamName())) {
                        request = new FileRequest(node, t);
                        break;
                    }
        }

        return request;
    }

    public abstract boolean isCrooked();
}
