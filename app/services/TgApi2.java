package services;

import akka.actor.ActorSystem;
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
import scala.concurrent.ExecutionContext;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
    private final ActorSystem actorSystem;
    private final ExecutionContext ec;

    @Inject
    public TgApi2(final Config config, final WSClient ws, final ActorSystem actorSystem, final ExecutionContext ec) {
        this.ws = ws;
        this.actorSystem = actorSystem;
        this.ec = ec;
        apiUrl = config.getString("service.bot.api_url");
    }

    public void sendOrUpdate(final String text, final String format, final ReplyMarkup replyMarkup, final long updateMessageId, final User user,
                             final Consumer<Long> msgIdConsumer) {
        if (updateMessageId > 0) {
            final ObjectNode node = Json.newObject();
            node.put("chat_id", user.getId());
            node.put("text", text);
            node.put("message_id", updateMessageId);
            if (format != null)
                node.put("parse_mode", format);
            if (replyMarkup != null)
                node.set("reply_markup", Json.toJson(replyMarkup));

            actorSystem.scheduler().scheduleOnce(Duration.of(user.getDelay(), ChronoUnit.MILLIS), () ->
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
                                    sendMessage(text, format, user, replyMarkup, msgIdConsumer);
                                else
                                    msgIdConsumer.accept(msgId);
                            }), ec);
        } else
            sendMessage(text, format, user, replyMarkup, msgIdConsumer);
    }

    public void deleteMessage(final long messageId, final User user) {
        actorSystem.scheduler().scheduleOnce(Duration.of(user.getDelay(), ChronoUnit.MILLIS), () -> ws.url(apiUrl + "deleteMessage")
                .setContentType(jsonType)
                .post("{\"chat_id\":" + user.getId() + ",\"message_id\":" + messageId + "}"), ec);
    }

    public void ask(final String question, final User user, final Consumer<Long> msgIdConsumer) {
        sendMessage(question, null, user, new ForceReply(), msgIdConsumer);
    }

    public void sendPlainText(final String text, final User user, final Consumer<Long> msgIdConsumer) {
        sendMessage(text, null, user, null, msgIdConsumer);
    }

    private void sendMessage(final String text, final String format, final User user, final ReplyMarkup replyMarkup, final Consumer<Long> msgIdConsumer) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", user.getId());
        node.put("text", text);
        if (format != null)
            node.put("parse_mode", format);
        if (replyMarkup != null)
            node.set("reply_markup", Json.toJson(replyMarkup));

        actorSystem.scheduler().scheduleOnce(Duration.of(user.getDelay(), ChronoUnit.MILLIS), () -> ws.url(apiUrl + "sendMessage")
                .post(node)
                .thenApply(wsr -> wsr.asJson().get("result").get("message_id").asLong())
                .thenAccept(msgIdConsumer), ec);
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

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "answerCallbackQuery").post(node));
    }

    public void sendMedia(final TFile media, final String caption, final ReplyMarkup replyMarkup, final User user, final Consumer<Long> msgIdConsumer) {
        if (media.getType() == ContentType.DIR || media.getType() == ContentType.LABEL)
            return;

        final ObjectNode node = Json.newObject();
        node.put("chat_id", user.getId());
        node.put(media.getType().getParamName(), media.getRefId());
        if (!isEmpty(caption))
            node.put("caption", caption);
        if (replyMarkup != null)
            node.set("reply_markup", Json.toJson(replyMarkup));

        actorSystem.scheduler().scheduleOnce(Duration.of(user.getDelay(), ChronoUnit.MILLIS), () -> ws.url(apiUrl + media.getType().getUrlPath())
                .post(node)
                .thenApply(wsr -> wsr.asJson().get("result").get("message_id").asLong())
                .thenAccept(msgIdConsumer), ec);
    }
}
