package services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import model.TFile;
import model.telegram.api.*;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import services.TgApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class TgApiReal implements TgApi {
    private static final Logger.ALogger logger = Logger.of(TgApiReal.class);

    private final String apiUrl;
    private final WSClient ws;

    @Inject
    public TgApiReal(final Config config, final WSClient ws) {
        this.ws = ws;
        apiUrl = config.getString("service.bot.api_url");
    }

    @Override
    public void sendFile(final TFile file, final long chatId) {
        if (file == null)
            return;

        final ObjectNode node = Json.newObject();
        node.put("chat_id", chatId);
        node.put(file.getType().getParamName(), file.getRefId());

        ws.url(apiUrl + file.getType().getUrlPath())
                .post(node)
                .thenAccept(wsr -> logger.debug("API call: send" + file.getType().getUrlPath() + "\n" + node + "\nResponse: " + wsr.getBody()));
    }

    @Override
    public CompletionStage<ApiMessageReply> sendMessage(final TextRef text) {
        if (isEmpty(text) || isEmpty(text.getText()))
            return CompletableFuture.completedFuture(null);

        final JsonNode node = Json.toJson(text);

        return ws.url(apiUrl + "sendMessage")
                .post(node)
                .thenApply(wsr -> {
                    logger.debug("API call: sendMessage\n" + node + "\nResponse: " + wsr.getBody());

                    return Json.fromJson(wsr.asJson(), ApiMessageReply.class);
                });
    }

    @Override
    public CompletionStage<ApiMessageReply> updateMessage(final UpdateMessage update) {
        if (isEmpty(update) || isEmpty(update.getText()))
            return CompletableFuture.completedFuture(null);

        final JsonNode node = Json.toJson(update);

        return ws.url(apiUrl + "editMessageText")
                .post(node)
                .thenApply(wsr -> {
                    logger.debug("API call: editMessageText\n" + node + "\nResponse: " + wsr.getBody());

                    return Json.fromJson(wsr.asJson(), ApiMessageReply.class);
                });
    }

    @Override
    public CompletionStage<Void> deleteMessage(final DeleteMessage deleteMessage) {
        if (isEmpty(deleteMessage) || deleteMessage.getMessageId() <= 0)
            return CompletableFuture.completedFuture(null);

        final JsonNode node = Json.toJson(deleteMessage);

        return ws.url(apiUrl + "deleteMessage")
                .post(node)
                .thenApply(wsr -> {
                    logger.debug("API call: deleteMessage\n" + node + "\nResponse: " + wsr.getBody());
                    
                    return null;
                });
    }

    @Override
    public void sendCallbackAnswer(final CallbackAnswer callbackAnswer) {
        if (isEmpty(callbackAnswer) || callbackAnswer.getCallbackId() <= 0)
            return;

        final JsonNode node = Json.toJson(callbackAnswer);

        ws.url(apiUrl + "answerCallbackQuery")
                .post(node)
                .thenAccept(wsr -> logger.debug("API call: answerCallbackQuery\n" + node + "\nResponse: " + wsr.getBody()));
    }

    @Override
    public CompletionStage<ApiMessageReply> sendEditMedia(final EditMedia media) {
        if (isEmpty(media) || media.getMessageId() <= 0)
            return CompletableFuture.completedFuture(null);

        final JsonNode node = Json.toJson(media);

        return ws.url(apiUrl + "editMessageMedia")
                .post(node)
                .thenApply(wsr -> {
                    logger.debug("API call: editMessageMedia\n" + node + "\nResponse: " + wsr.getBody());

                    return Json.fromJson(wsr.asJson(), ApiMessageReply.class);
                });
    }

    @Override
    public CompletionStage<ApiMessageReply> editCaption(final EditCaption caption) {
        if (isEmpty(caption) || caption.getMessageId() <= 0)
            return CompletableFuture.completedFuture(null);

        final JsonNode node = Json.toJson(caption);

        return ws.url(apiUrl + "editMessageCaption")
                .post(node)
                .thenApply(wsr -> {
                    logger.debug("API call: editMessageCaption\n" + node + "\nResponse: " + wsr.getBody());

                    return Json.fromJson(wsr.asJson(), ApiMessageReply.class);
                });
    }
}
