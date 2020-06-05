package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import model.TFile;
import model.telegram.ContentType;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
public class TgApi {
    private static final Logger.ALogger logger = Logger.of(TgApi.class);
    private static final String jsonType = "application/json";

    private final String apiUrl;
    private final WSClient ws;
    public static final String formatMd = "Markdown", formatMd2 = "MarkdownV2", formatHtml = "HTML";

    @Inject
    public TgApi(final Config config, final WSClient ws) {
        this.ws = ws;
        apiUrl = config.getString("service.bot.api_url");
    }

    public CompletionStage<Reply> editMessageText(final long chatId, final long updateMessageId, final String text, final String format, final JsonNode replyMarkup) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", chatId);
        node.put("text", text);
        node.put("message_id", updateMessageId);
        node.put("disable_web_page_preview", true);
        if (format != null)
            node.put("parse_mode", format);
        if (replyMarkup != null)
            node.set("reply_markup", replyMarkup);

        return ws.url(apiUrl + "editMessageText")
                .post(node)
                .thenApply(wsr -> {
                    final JsonNode j = wsr.asJson();

                    if (j.get("ok").asBoolean())
                        return new Reply(updateMessageId);

                    return new Reply(j.get("description").asText());
                })
                .exceptionally(e -> {
                    logger.error(e.getMessage(), e);
                    return new Reply(e.getMessage());
                });
    }

    public CompletionStage<Reply> deleteMessage(final long messageId, final long chatId) {
        return ws.url(apiUrl + "deleteMessage")
                .setContentType(jsonType)
                .post("{\"chat_id\":" + chatId + ",\"message_id\":" + messageId + "}")
                .thenApply(wsr -> {
                    final JsonNode j = wsr.asJson();

                    if (j.get("ok").asBoolean())
                        return new Reply(0);

                    return new Reply(j.get("description").asText());
                })
                .exceptionally(e -> {
                    logger.error(e.getMessage(), e);
                    return new Reply(e.getMessage());
                });
    }

    public CompletionStage<Reply> sendMessage(final String text, final String format, final long chatId, final JsonNode replyMarkup) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", chatId);
        node.put("text", text);
        node.put("disable_web_page_preview", true);
        if (format != null)
            node.put("parse_mode", format);
        if (replyMarkup != null)
            node.set("reply_markup", replyMarkup);

        return ws.url(apiUrl + "sendMessage")
                .post(node)
                .thenApply(wsr -> {
                    final JsonNode j = wsr.asJson();

                    if (j.get("ok").asBoolean())
                        return new Reply(j.get("result").get("message_id").asLong());

                    return new Reply(j.get("description").asText());
                })
                .exceptionally(e -> {
                    logger.error(e.getMessage(), e);
                    return new Reply(e.getMessage());
                });
    }

    public void sendCallbackAnswer(final String text, final long callbackId, final boolean alert, final int cacheTime) {
        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", callbackId);
        node.put("text", text);
        node.put("show_alert", alert);
        node.put("cache_time", cacheTime);

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "answerCallbackQuery").post(node));
    }

    public CompletionStage<Reply> sendMedia(final TFile media, final String caption, final JsonNode replyMarkup, final long userId) {
        if (media.getType() == ContentType.DIR || media.getType() == ContentType.LABEL)
            return CompletableFuture.completedFuture(new Reply(media.getType().name()));

        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
        node.put(media.getType().getParamName(), media.getRefId());
        if (!isEmpty(caption))
            node.put("caption", caption);
        if (replyMarkup != null)
            node.set("reply_markup", replyMarkup);

        return ws.url(apiUrl + media.getType().getUrlPath())
                .post(node)
                .thenApply(wsr -> {
                    final JsonNode j = wsr.asJson();

                    if (j.get("ok").asBoolean())
                        return new Reply(j.get("result").get("message_id").asLong());

                    return new Reply(j.get("description").asText());
                })
                .exceptionally(e -> {
                    logger.error(e.getMessage(), e);
                    return new Reply(e.getMessage());
                });
    }

    public static class Reply {
        public final boolean ok;
        public final long messageId;
        public final long indate;
        public final String desc;

        public Reply(final long messageId) {
            ok = true;
            this.messageId = messageId;
            indate = System.currentTimeMillis();
            desc = "";
        }

        public Reply(final String desc) {
            ok = false;
            this.desc = desc;
            messageId = indate = 0;
        }
    }
}
