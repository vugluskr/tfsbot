package services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.ForceReply;
import model.telegram.api.ReplyMarkup;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import utils.LangMap;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static utils.LangMap.v;
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

    @Inject
    public TgApi(final Config config, final WSClient ws) {
        this.ws = ws;
        apiUrl = config.getString("service.bot.api_url");
    }

    public void sendOrUpdate(final String text, final String format, final ReplyMarkup replyMarkup, final long updateMessageId, final long chatId,
                             final Consumer<Long> msgIdConsumer) {
        if (updateMessageId > 0) {
            final ObjectNode node = Json.newObject();
            node.put("chat_id", chatId);
            node.put("text", text);
            node.put("message_id", updateMessageId);
            node.put("disable_web_page_preview", true);
            if (format != null)
                node.put("parse_mode", format);
            if (replyMarkup != null)
                node.set("reply_markup", Json.toJson(replyMarkup));

            CompletableFuture.runAsync(() ->
                    ws.url(apiUrl + "editMessageText")
                            .post(node)
                            .thenApply(wsr -> {
                                logger.debug("UpdateMessageText:\nrequest: " + node + "\nresponse: " + wsr.getBody());
                                return wsr;
                            })
                            .thenApply(wsr -> wsr.asJson().get("ok").asBoolean() ? wsr.asJson().get("result").get("message_id").asLong() :
                                    wsr.asJson().get("description").asText().contains("are exactly the same") ? updateMessageId : 0)// небольшой хак, если тг отвечает, что сообщение одинаковое - просто ничо делать не будем
                            .thenAccept(msgId -> {
                                if (msgId <= 0)
                                    sendMessage(text, format, chatId, replyMarkup, msgIdConsumer);
                                else
                                    msgIdConsumer.accept(msgId);
                            })
                            .exceptionally(e -> {
                                logger.error(e.getMessage(), e);
                                return null;
                            }));
        } else
            sendMessage(text, format, chatId, replyMarkup, msgIdConsumer);
    }

    public void deleteMessage(final long messageId, final long userId) {
        CompletableFuture.runAsync(() -> ws.url(apiUrl + "deleteMessage")
                .setContentType(jsonType)
                .post("{\"chat_id\":" + userId + ",\"message_id\":" + messageId + "}")
                .thenAccept(wsr -> {
                    try {
                        if (!wsr.asJson().get("ok").asBoolean())
                            logger.error("Error complete delete message #" + messageId + ": " + wsr.getBody());
                    } catch (final Exception e) {
                        logger.error("Error complete delete message #" + messageId + ": " + e.getMessage(), e);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error(throwable.getMessage(), throwable);
                    return null;
                }));
    }

    public void ask(final LangMap.Value question, final User user, final Consumer<Long> msgIdConsumer, final Object... args) {
        sendMessage(v(question, user, args), null, user.getId(), new ForceReply(), msgIdConsumer);
    }

    public void sendPlainText(final LangMap.Value value, final User user, final Consumer<Long> msgIdConsumer, final Object... args) {
        sendMessage(v(value, user, args), null, user.getId(), null, msgIdConsumer);
    }

    private void sendMessage(final String text, final String format, final long chatId, final ReplyMarkup replyMarkup, final Consumer<Long> msgIdConsumer) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", chatId);
        node.put("text", text);
        if (format != null)
            node.put("parse_mode", format);
        if (replyMarkup != null)
            node.set("reply_markup", Json.toJson(replyMarkup));

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "sendMessage")
                .post(node)
                .thenApply(wsr -> wsr.asJson().get("result").get("message_id").asLong())
                .thenAccept(msgIdConsumer));
    }

    public void sendCallbackAnswer(final LangMap.Value text, final long callbackId, final boolean alert, final int cacheTime, final User user, final Object... args) {
        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", callbackId);
        node.put("text", v(text, user, args));
        node.put("show_alert", alert);
        node.put("cache_time", cacheTime);

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "answerCallbackQuery")
                .post(node));
    }

    public void sendMedia(final TFile media, final String caption, final ReplyMarkup replyMarkup, final long userId, final Consumer<Long> msgIdConsumer) {
        if (media.getType() == ContentType.DIR || media.getType() == ContentType.LABEL)
            return;

        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
        node.put(media.getType().getParamName(), media.getRefId());
        if (!isEmpty(caption))
            node.put("caption", caption);
        if (replyMarkup != null)
            node.set("reply_markup", Json.toJson(replyMarkup));

        CompletableFuture.runAsync(() -> ws.url(apiUrl + media.getType().getUrlPath())
                .post(node)
                .thenApply(wsr -> wsr.asJson().get("result").get("message_id").asLong())
                .thenAccept(msgIdConsumer));
    }

    public void sendCallbackAnswer(final LangMap.Value text, final long callbackId, final User user, final Object... args) {
        sendCallbackAnswer(text, callbackId, false, 0, user, args);
    }
}
