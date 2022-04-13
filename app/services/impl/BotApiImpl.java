package services.impl;

import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.ContentType;
import model.TFile;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
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
    public CompletionStage<byte[]> downloadFile(final String refId) {
        if (isEmpty(refId)) {
            logger.warn("Not getting file with empty id");
            return CompletableFuture.completedFuture(null);
        }

        final ObjectNode node = Json.newObject();

        node.put("file_id", refId);

        return ws.url(apiUrl + "getFile")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(wsr -> wsr.asJson().get("result").get("file_path").asText())
                .thenCompose(path -> ws.url(apiUrl.replace(".org/bot", ".org/file/bot") + path).get())
                .thenApply(WSResponse::asByteArray);
    }

    @Override
    public CompletionStage<Reply> sendText(final TextMessage msg) {
        if (isEmpty(msg.body)) {
            logger.warn("Not send empty text to " + msg.chat);
            return CompletableFuture.completedFuture(new Reply(false));
        }

        if (msg.chat == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false));
        }

        final ObjectNode node = Json.newObject();

        msg.chat.set("chat_id", node);
        node.put("text", msg.body);
        node.put("disable_web_page_preview", true);

        if (msg.mode != null)
            node.put("parse_mode", msg.mode.asText());
        if (msg.kbd != null)
            node.set("reply_markup", msg.kbd.toJson());

        return ws.url(apiUrl + "sendMessage")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Reply> sendMedia(final MediaMessage msg) {
        if (msg.rawMedia != null)
            return uploadMedia(msg);

        if (msg.chat == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false));
        }

        if (msg.media == null) {
            logger.warn("Not send empty media to " + msg.chat);
            return CompletableFuture.completedFuture(new Reply(false));
        }

        if (msg.media.type == null || msg.media.type == ContentType.DIR || msg.media.type == ContentType.LABEL) {
            logger.warn("Not send this media to " + msg.chat + " :: " + msg.media.type);
            return CompletableFuture.completedFuture(new Reply(false));
        }

        final ObjectNode node = msg.media.type == ContentType.CONTACT ? (ObjectNode) Json.parse(msg.media.getRefId()) : Json.newObject();
        msg.chat.set("chat_id", node);
        node.put(msg.media.type.getParamName(), msg.media.getRefId());

        if (!isEmpty(msg.caption)) {
            node.put("caption", msg.caption);

            if (msg.mode != null)
                node.put("parse_mode", msg.mode.asText());
        }

        if (msg.kbd != null)
            node.set("reply_markup", msg.kbd.toJson());

        return ws.url(apiUrl + msg.media.type.getUrlPath())
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Void> sendReaction(final ReactionMessage msg) {
        if (isEmpty(msg.text)) {
            logger.warn("Not send empty reaction to " + msg.queryId);
            return CompletableFuture.completedFuture(null);
        }

        if (msg.queryId <= 0) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(null);
        }

        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", msg.queryId);
        node.put("text", msg.text);
        node.put("show_alert", msg.alert);
        node.put("cache_time", 0);

        return ws.url(apiUrl + "answerCallbackQuery")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenAccept(wsResponse -> {});
    }

    private CompletionStage<Reply> uploadMedia(final MediaMessage msg) {
        if (msg.chat == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(new Reply(false));
        }

        if (msg.rawMedia == null || !msg.rawMedia.isComplete()) {
            logger.warn("Not send empty media to " + msg.chat);
            return CompletableFuture.completedFuture(new Reply(false));
        }

        final List<Http.MultipartFormData.Part<?>> parts = new ArrayList<>(0);

        parts.add(new Http.MultipartFormData.FilePart<>(msg.rawMedia.type.getParamName(), msg.rawMedia.filename, msg.rawMedia.mimeType, msg.rawMedia.src));
        parts.add(new Http.MultipartFormData.DataPart("chat_id", msg.chat.toString()));
        if (!isEmpty(msg.caption)) {
            parts.add(new Http.MultipartFormData.DataPart("caption", msg.caption));

            if (msg.mode != null)
                parts.add(new Http.MultipartFormData.DataPart("parse_mode", msg.mode.asText()));
        }
        if (msg.kbd != null)
            parts.add(new Http.MultipartFormData.DataPart("reply_markup", msg.kbd.toJson().toString()));

        return ws.url(apiUrl + msg.rawMedia.type.getUrlPath())
                .setRequestFilter(new WsCurlLogger())
                .post(Source.from(parts))
                .thenApply(Reply::new);
    }

    @Override
    public CompletionStage<Boolean> editText(final TextMessage msg, final long editMessageId) {
        if (msg.chat == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(false);
        }

        if (isEmpty(msg.body)) {
            logger.warn("Not send empty text to " + msg.chat);
            return CompletableFuture.completedFuture(false);
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(false);
        }

        final ObjectNode node = Json.newObject();
        msg.chat.set("chat_id", node);
        node.put("text", msg.body);
        node.put("message_id", editMessageId);
        node.put("disable_web_page_preview", true);
        if (msg.mode != null)
            node.put("parse_mode", msg.mode.asText());
        if (msg.kbd != null)
            node.set("reply_markup", msg.kbd.toJson());

        return ws.url(apiUrl + "editMessageText")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new)
                .thenApply(Reply::isOk);
    }

    @Override
    public CompletionStage<Boolean> editMedia(final MediaMessage msg, final long editMessageId) {
        if (msg.chat == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(false);
        }

        if (isEmpty(msg.caption) && msg.media == null) {
            logger.warn("Not send empty caption to " + msg.chat);
            return CompletableFuture.completedFuture(false);
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(false);
        }

        final ObjectNode node = Json.newObject();
        msg.chat.set("chat_id", node);
        node.put("message_id", editMessageId);
        node.put("caption", msg.caption);

        if (msg.mode != null)
            node.put("parse_mode", msg.mode.asText());

        if (msg.kbd != null)
            node.set("reply_markup", msg.kbd.toJson());

        if (msg.media != null)
            return editMedia(msg.media, msg.chat, editMessageId)
                    .thenCompose(mediaUpdated -> doCaption(node));

        return doCaption(node);
    }

    private CompletionStage<Boolean> doCaption(final JsonNode node) {
        return ws.url(apiUrl + "editMessageCaption")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(Reply::new)
                .thenApply(Reply::isOk);
    }

    private CompletionStage<Boolean> editMedia(final TFile media, final Chat target, final long editMessageId) {
        if (media == null) {
            logger.warn("Not send empty media to " + target);
            return CompletableFuture.completedFuture(false);
        }

        if (target == null) {
            logger.warn("Not send anything to null target");
            return CompletableFuture.completedFuture(false);
        }

        if (media.type == null || media.type == ContentType.DIR || media.type == ContentType.LABEL) {
            logger.warn("Not send this media to " + target + " :: " + media.type);
            return CompletableFuture.completedFuture(false);
        }

        if (editMessageId <= 0) {
            logger.warn("Not editing message #0");
            return CompletableFuture.completedFuture(false);
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
                .thenApply(Reply::new)
                .thenApply(Reply::isOk);
    }

    @Override
    public CompletionStage<Void> dropMessage(final long messageId, final Chat target) {
        if (messageId <= 0) {
            logger.warn("Not deleting message #0");
            return CompletableFuture.completedFuture(null);
        }

        if (target == null) {
            logger.warn("Not delete anything of null target");
            return CompletableFuture.completedFuture(null);
        }

        final ObjectNode node = Json.newObject();
        target.set("chat_id", node);

        node.put("message_id", messageId);

        return ws.url(apiUrl + "deleteMessage")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(wsResponse -> null);
    }
}
