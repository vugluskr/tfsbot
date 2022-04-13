package model.request;

import com.fasterxml.jackson.databind.JsonNode;
import model.ContentType;
import model.TUser;

import static model.ContentType.media;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 14:56
 * tfs ☭ sweat and blood
 */
public abstract class TgRequest {
    public final TUser user;

    protected TgRequest(final JsonNode node) {
        user = new TUser(node);
    }

    public static TgRequest resolve(final JsonNode node) {
        TgRequest request = null;

        if (node.has("callback_query"))
            request = new CallbackRequest(node.get("callback_query"));
        else if (node.has("message")) {
            if (node.get("message").has("text")) {
                final String t = notNull(node.get("message").get("text").asText());

                if (t.startsWith("/"))
                    request = new CmdRequest(node.get("message"), t);
                else
                    request = new TextRequest(node.get("message"), t);
            } else
                for (final ContentType t : media)
                    if (node.get("message").has(t.getParamName())) {
                        request = new FileRequest(node.get("message"), t);
                        break;
                    }
        }

        return request;
    }

    public abstract boolean isCrooked();
}
