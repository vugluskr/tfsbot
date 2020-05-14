package services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import model.TFile;
import model.telegram.ContentType;
import model.telegram.api.ForceReply;
import model.telegram.api.ReplyMarkup;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
public class TgApi2 {
    private static final Logger.ALogger logger = Logger.of(TgApi2.class);
    private static final String jsonType = "application/json";

    private final String apiUrl;
    private final WSClient ws;

    @Inject
    public TgApi2(final Config config, final WSClient ws) {
        this.ws = ws;
        apiUrl = config.getString("service.bot.api_url");
    }

    public void sendOrUpdate(final String text, final String format, final ReplyMarkup replyMarkup, final long updateMessageId, final long userId,
                             final Consumer<Long> msgIdConsumer) {
        if (updateMessageId > 0) {
            final ObjectNode node = Json.newObject();
            node.put("chat_id", userId);
            node.put("text", text);
            node.put("message_id", updateMessageId);
            if (format != null)
                node.put("parse_mode", format);
            if (replyMarkup != null)
                node.set("reply_markup", Json.toJson(replyMarkup));

            CompletableFuture.runAsync(() -> ws.url(apiUrl + "editMessageText")
                    .post(node)
                    .thenApply(wsr -> {
                        logger.debug("UpdateMessageText:\nrequest: " + node + "\nresponse: " + wsr.getBody());
                        return wsr;
                    })
                    .thenApply(wsr -> wsr.asJson().get("ok").asBoolean() ? wsr.asJson().get("result").get("message_id").asLong() :
                            wsr.asJson().get("description").asText().contains("are exactly the same") ? updateMessageId : 0)// небольшой хак, если тг отвечает, что сообщение одинаковое - просто ничо делать не будем
                    .thenAccept(msgId -> {
                        if (msgId <= 0)
                            sendMessage(text, format, userId, replyMarkup, msgIdConsumer);
                        else
                            msgIdConsumer.accept(msgId);
                    }));
        } else
            sendMessage(text, format, userId, replyMarkup, msgIdConsumer);
    }

    public void deleteMessage(final long messageId, final long userId) {
        CompletableFuture.runAsync(() -> ws.url(apiUrl + "deleteMessage")
                .setContentType(jsonType)
                .post("{\"chat_id\":" + userId + ",\"message_id\":" + messageId + "}"));
    }

    public void ask(final String question, final long userId, final Consumer<Long> msgIdConsumer) {
        sendMessage(question, null, userId, new ForceReply(), msgIdConsumer);
    }

    public void sendPlainText(final String text, final long userId, final Consumer<Long> msgIdConsumer) {
        sendMessage(text, null, userId, null, msgIdConsumer);
    }

    private void sendMessage(final String text, final String format, final long userId, final ReplyMarkup replyMarkup, final Consumer<Long> msgIdConsumer) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
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

    public void sendCallbackAnswer(final String text, final long callbackId) {
        sendCallbackAnswer(text, callbackId, false, isEmpty(text) ? 0 : 4);
    }

    public void sendCallbackAnswer(final String text, final long callbackId, final boolean alert, final int cacheTime) {
        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", callbackId);
        node.put("text", text);
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
}
