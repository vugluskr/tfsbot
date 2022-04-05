package services.impl;

import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.ContentType;
import model.TFile;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.WsCurlLogger;
import play.mvc.Http;
import services.BotApi;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 13:32
 * tfs ☭ sweat and blood
 */

public class BotApiImpl implements BotApi {
    private static final Logger.ALogger logger = Logger.of(BotApiImpl.class);

    private final String apiUrl;

    @Inject
    private WSClient ws;

    @Inject
    public BotApiImpl(final Config config) {
        apiUrl = config.getString("service.bot.api_url");
    }

    @Override
    public CompletionStage<Reply> sendText(final String body, final ParseMode mode, final Chat target, final Keyboard kbd) {
        if (isEmpty(body)) {
            logger.warn("Not send empty text to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty text"));
        }

        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        final ObjectNode node = Json.newObject();

        target.set("chat_id", node);
        node.put("text", body);
        node.put("disable_web_page_preview", true);

        if (mode != null)
            node.put("parse_mode", mode.asText());
        if (kbd != null)
            node.set("reply_markup", kbd.toJson());

        return ws.url(apiUrl + "sendMessage")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> sendMedia(final TFile media, final String caption, final ParseMode mode, final Chat target, final Keyboard kbd) {
        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (media == null) {
            logger.warn("Not send empty media to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty media"));
        }

        if (media.type == null || media.type == ContentType.DIR || media.type == ContentType.LABEL) {
            logger.warn("Not send this media to " + target + " :: " + media.type);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send this media :: " + media.type));
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);
        node.put(media.type.getParamName(), media.getRefId());

        if (!isEmpty(caption)) {
            node.put("caption", caption);

            if (mode != null)
                node.put("parse_mode", mode.asText());
        }

        if (kbd != null)
            node.set("reply_markup", kbd.toJson());

        return ws.url(apiUrl + media.type.getUrlPath())
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> sendReaction(final String reaction, final boolean asAlert, final long queryId, final int showTime) {
        if (isEmpty(reaction)) {
            logger.warn("Not send empty reaction to " + queryId);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty reaction"));
        }

        if (queryId <= 0) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", queryId);
        node.put("text", reaction);
        node.put("show_alert", asAlert);
        node.put("cache_time", showTime);

        return ws.url(apiUrl + "answerCallbackQuery")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> uploadMedia(final RawMedia media, final String caption, final ParseMode mode, final Chat target, final Keyboard kbd) {
        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (media == null || !media.isComplete()) {
            logger.warn("Not send empty media to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty media"));
        }

        final List<Http.MultipartFormData.Part<?>> parts = new ArrayList<>(0);

        parts.add(new Http.MultipartFormData.FilePart<>(media.type.getParamName(), media.filename, media.mimeType, media.src));
        parts.add(new Http.MultipartFormData.DataPart("chat_id", target.toString()));
        if (!isEmpty(caption)) {
            parts.add(new Http.MultipartFormData.DataPart("caption", caption));

            if (mode != null)
                parts.add(new Http.MultipartFormData.DataPart("parse_mode", mode.asText()));
        }
        if (kbd != null)
            parts.add(new Http.MultipartFormData.DataPart("reply_markup", kbd.toJson().toString()));

        return ws.url(apiUrl + media.type.getUrlPath())
                .setRequestFilter(new WsCurlLogger())
                .post(Source.from(parts))
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> editBody(final String body, final ParseMode mode, final Chat target, final long editMessageId) {
        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (isEmpty(body)) {
            logger.warn("Not send empty text to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty text"));
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant edit message #0"));
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);
        node.put("text", body);
        node.put("message_id", editMessageId);
        node.put("disable_web_page_preview", true);
        if (mode != null)
            node.put("parse_mode", mode.asText());

        return ws.url(apiUrl + "editMessageText")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> editCaption(final String caption, final ParseMode mode, final Chat target, final long editMessageId) {
        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (isEmpty(caption)) {
            logger.warn("Not send empty caption to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty caption"));
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant edit message #0"));
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);
        node.put("message_id", editMessageId);
        node.put("caption", caption);

        if (mode != null)
            node.put("parse_mode", mode.asText());

        return ws.url(apiUrl + "editMessageCaption")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> editKeyboard(final Keyboard kbd, final Chat target, final long editMessageId) {
        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant edit message #0"));
        }

        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (kbd == null) {
            logger.warn("Not editing null keyboard");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant edit null keyboard"));
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);

        node.put("message_id", editMessageId);
        node.set("reply_markup", kbd.toJson());

        return ws.url(apiUrl + "editMessageReplyMarkup")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> editMedia(final TFile media, final Chat target, final long editMessageId) {
        if (media == null) {
            logger.warn("Not send empty media to " + target);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send empty media"));
        }

        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send anything to null target"));
        }

        if (media.type == null || media.type == ContentType.DIR || media.type == ContentType.LABEL) {
            logger.warn("Not send this media to " + target + " :: " + media.type);
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant send this media :: " + media.type));
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant edit message #0"));
        }

        final ObjectNode node = Json.newObject();
        final ObjectNode mediaNode = node.with("media");

        mediaNode.put("type", media.type.getParamName());
        mediaNode.put("media", media.getRefId());
        node.put("message_id", editMessageId);
        target.set("chat_id", node);

        return ws.url(apiUrl + "editMessageMedia")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> dropMessage(final Chat target, final long messageId) {
        if (messageId <= 0) {
            logger.warn("Not deleting message #0");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant delete message #0"));
        }

        if (target == null) {
            logger.warn("Not delete anything of null target");
            return CompletableFuture.completedFuture(new Reply(false).withDesc("Cant delete anything of null target"));
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);

        node.put("message_id", messageId);

        return ws.url(apiUrl + "deleteMessage")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }
}
