package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import model.*;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.libs.ws.ahc.WsCurlLogger;
import sql.MediaMessageMapper;
import sql.TFileSystem;
import utils.LangMap;
import utils.TextUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
@Singleton
public class TgApi {
    private static final Logger.ALogger logger = Logger.of(TgApi.class);
    public static final Keyboard voidKbd = new Keyboard().button(CommandType.Void.b());

    private final String apiUrl;
    private final WSClient ws;
    private final TFileSystem fs;
    private final MediaMessageMapper mediaMessageMapper;

    @Inject
    public TgApi(final Config config, final WSClient ws, final TFileSystem fs, final MediaMessageMapper mediaMessageMapper) {
        this.ws = ws;
        this.fs = fs;
        apiUrl = config.getString("service.bot.api_url");
        this.mediaMessageMapper = mediaMessageMapper;
    }

    public CompletionStage<List<JsonNode>> getUpdates(Long lastId) {
        return ws.url(apiUrl + "getUpdates" + (getLong(lastId) > 0 ? "?offset=" + lastId : ""))
                .setRequestFilter(new WsCurlLogger())
                .get()
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> {
                    if (jsonNode.has("result") && jsonNode.get("result").isArray())
                        return new ObjectMapper().convertValue(jsonNode.get("result"), new TypeReference<List<JsonNode>>() {});

                    return Collections.emptyList();
                }); // exceptionally будет обработано в WsCurlLogger
    }

    public void cleanup(final long userId) {
        final Collection<Long> wins = fs.selectServiceWindows(userId);
        wins.forEach(msgId -> CompletableFuture.runAsync(() -> deleteMessage(msgId, userId)));
        if (!wins.isEmpty())
            fs.deleteServiceWindows(userId);
    }

    public void dialog(final LangMap.Value text, final User user, final Object... args) {
        dialog(text, user, voidKbd, args);
    }

    public void dialog(final LangMap.Value text, final User user, final Keyboard kbd, final Object... args) {
        CompletableFuture.runAsync(() -> sendText(TextUtils.escapeMd(v(text, user, args)), ParseMode.md2, kbd.toJson(), user.id)
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.id)));
    }

    public void dialogUnescaped(final LangMap.Value text, final User user, final Keyboard kbd, final Object... args) {
        CompletableFuture.runAsync(() -> sendText(v(text, user, args), ParseMode.md2, kbd.toJson(), user.id)
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.id)));
    }

    public void sendContent(final TFile file, final String body, final String format, final Keyboard keyboard, final User user) {
        sendContent(file, body, format, keyboard, user, 0);
    }

    private void sendContent(final TFile file, final String body, final String format, final Keyboard keyboard, final User user, final int cnt) {
        final Function<Throwable, Void> fuckup = e -> {
            logger.error("[#" + cnt + "] " + e.getMessage(), e);
            return null;
        };

        if (diffState(file, user.lastRefId)) {
            final long toDel = user.lastMessageId;
            CompletableFuture.runAsync(() -> deleteMessage(toDel, user.id));
            user.lastMessageId = 0;
        }

        final String ks = keyboard == null ? null : keyboard.toJson().toString();
        final Consumer<TgApi.Reply> sendSuccessConsumer = reply -> {
            if (reply.ok) {
                user.lastMessageId = reply.messageId;
                fs.updateLastMessageId(reply.messageId, user.id);
            } else {
                logger.error("Cant send content: " + reply.desc);
            }
        };

        if (user.lastMessageId > 0) {
            final boolean sameKbd = Objects.equals(ks, user.lastKeyboard);
            final boolean sameFile = Objects.equals(file == null ? "" : file.getRefId(), user.lastRefId);
            final boolean sameText = Objects.equals(body, user.lastText);

            if (sameFile && sameText && sameKbd)
                return;

            final Consumer<Reply> editSuccessConsumer = reply -> {
                if (!reply.ok) {
                    CompletableFuture.runAsync(() -> deleteMessage(user.lastMessageId, user.id));
                    user.lastMessageId = 0;
                    fs.updateLastMessageId(0, user.id);
                    if (cnt < 2)
                        sendContent(file, body, format, keyboard, user, cnt + 1);
                }
            };

            if (file != null) {
                if (sameFile) {
                    if (sameText)
                        CompletableFuture.runAsync(() ->
                                editKeyboard(keyboard == null ? Json.newObject() : keyboard.toJson(), user.id, user.lastMessageId)
                                        .thenAccept(editSuccessConsumer)
                                        .exceptionally(fuckup));
                    else
                        CompletableFuture.runAsync(() ->
                                editCaption(body, format, keyboard == null ? null : keyboard.toJson(), user.id, user.lastMessageId)
                                        .thenAccept(editSuccessConsumer)
                                        .exceptionally(fuckup));
                } else {
                    if (!notNull(user.lastRefId).isEmpty())
                        CompletableFuture.runAsync(() ->
                                editMedia(file.getRefId(), file.getType(), keyboard == null ? null : keyboard.toJson(), user.id, user.lastMessageId)
                                        .thenAccept(editSuccessConsumer)
                                        .exceptionally(fuckup));
                    else
                        CompletableFuture.runAsync(() ->
                                sendMedia(file.getRefId(), file.getType(), body, format, keyboard == null ? null : keyboard.toJson(), user.id)
                                        .thenAccept(sendSuccessConsumer)
                                        .exceptionally(fuckup));
                }
            } else {
                if (!sameKbd && sameText)
                    CompletableFuture.runAsync(() ->
                            editKeyboard(keyboard == null ? Json.newObject() : keyboard.toJson(), user.id, user.lastMessageId)
                                    .thenAccept(editSuccessConsumer)
                                    .exceptionally(fuckup));
                else
                    CompletableFuture.runAsync(() ->
                            editText(body, format, keyboard == null ? null : keyboard.toJson(), user.id, user.lastMessageId)
                                    .thenAccept(editSuccessConsumer)
                                    .exceptionally(fuckup));
            }
        } else {
            if (file != null)
                CompletableFuture.runAsync(() ->
                        sendMedia(file.getRefId(), file.getType(), body, format, keyboard == null ? null : keyboard.toJson(), user.id)
                                .thenAccept(sendSuccessConsumer)
                                .exceptionally(fuckup));
            else
                CompletableFuture.runAsync(() ->
                        sendText(body, format, keyboard == null ? null : keyboard.toJson(), user.id)
                                .thenAccept(sendSuccessConsumer)
                                .exceptionally(fuckup));
        }

        user.lastRefId = (file == null ? "" : file.getRefId());
        user.lastKeyboard = ks;
        user.lastText = body;
    }

    private boolean diffState(final TFile file, final String lastRefId) {
        return (file != null && notNull(lastRefId).isEmpty()) || (!notNull(lastRefId).isEmpty() && file == null);
    }

    public void deleteMessage(final long messageId, final long userId) {
        if (messageId > 0) {
            CompletableFuture.runAsync(() -> mediaMessageMapper.deleteMessageById(messageId, userId))
                    .thenCompose(unused ->
                            ws.url(apiUrl + "deleteMessage")
                                    .setRequestFilter(new WsCurlLogger())
                                    .setContentType("application/json").post("{\"chat_id\":" + userId + ",\"message_id\":" + messageId + "}"));
        }
    }

    public CompletionStage<Reply> sendText(final String text, final String format, final JsonNode replyMarkup, final long userId) {
        final ObjectNode node = Json.newObject();

        node.put("chat_id", userId);
        node.put("text", text);
        node.put("disable_web_page_preview", true);

        if (format != null)
            node.put("parse_mode", format);
        if (replyMarkup != null)
            node.set("reply_markup", replyMarkup);

        return doCall(node, "sendMessage");
    }

    public CompletionStage<Reply> editText(final String text, final String format, final JsonNode keyboard, final long userId, final long updateMessageId) {
        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
        node.put("text", text);
        node.put("message_id", updateMessageId);
        node.put("disable_web_page_preview", true);
        if (format != null)
            node.put("parse_mode", format);
        if (keyboard != null)
            node.set("reply_markup", keyboard);

        return ws.url(apiUrl + "editMessageText")
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(WSResponse::asJson)
                .thenApply(j -> {
                    if (j.get("ok").asBoolean())
                        return new Reply(updateMessageId);

                    return new Reply(j.get("description").asText());
                });
    }

    public void sendCallbackAnswer(final String text, final long callbackId, final boolean alert, final int cacheTime) {
        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", callbackId);
        node.put("text", text);
        node.put("show_alert", alert);
        node.put("cache_time", cacheTime);

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "answerCallbackQuery")
                .setRequestFilter(new WsCurlLogger())
                .post(node));
    }

    public CompletionStage<Reply> editKeyboard(final JsonNode keyboard, final long userId, final long messageId) {
        final ObjectNode node = Json.newObject();

        node.put("chat_id", userId);
        node.put("message_id", messageId);
        node.set("reply_markup", keyboard);

        return doCall(node, "editMessageReplyMarkup");
    }

    public CompletionStage<Reply> editCaption(final String caption, final String format, final JsonNode keyboard, final long userId, final long messageId) {
        final ObjectNode node = Json.newObject();

        node.put("chat_id", userId);
        node.put("message_id", messageId);
        node.put("caption", caption);

        if (format != null)
            node.put("parse_mode", format);
        if (keyboard != null)
            node.set("reply_markup", keyboard);

        return doCall(node, "editMessageCaption");
    }

    public CompletionStage<Reply> editMedia(final String refId, final ContentType type, final JsonNode keyboard, final long userId, final long messageId) {
        if (type == ContentType.DIR || type == ContentType.LABEL)
            return CompletableFuture.completedFuture(new Reply(type.name()));

        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
        node.put("message_id", messageId);
        final ObjectNode media = node.with("media");
        media.put("type", type.getParamName());
        media.put("media", refId);
        if (keyboard != null)
            node.set("reply_markup", keyboard);

        return doCall(node, "editMessageMedia");
    }

    public CompletionStage<Reply> sendMedia(final String refId, final ContentType type, final String caption, final String format, final JsonNode keyboard, final long userId) {
        if (type == ContentType.DIR || type == ContentType.LABEL)
            return CompletableFuture.completedFuture(new Reply(type.name()));

        final ObjectNode node = Json.newObject();
        node.put("chat_id", userId);
        if (type == ContentType.CONTACT) {
            final JsonNode c = Json.parse(refId);
            c.fieldNames().forEachRemaining(s -> node.set(s, c.get(s)));
        } else
            node.put(type.getParamName(), refId);
        if (!isEmpty(caption)) {
            node.put("caption", caption);

            if (!isEmpty(format))
                node.put("parse_mode", format);
        }
        if (keyboard != null)
            node.set("reply_markup", keyboard);

        return doCall(node, type.getUrlPath()).thenApply(reply -> {
            mediaMessageMapper.insertMessageId(reply.messageId, userId);
            return reply;
        });
    }

    public CompletionStage<JsonNode> upload(final File file) {
        return ws.url(apiUrl + "sendDocument")
                .setRequestFilter(new WsCurlLogger())
                .post(file)
                .thenApply(WSResponse::asJson);
    }

    private CompletionStage<Reply> doCall(final JsonNode node, final String partialUrl) {
        return ws.url(apiUrl + partialUrl)
                .setRequestFilter(new WsCurlLogger())
                .post(node)
                .thenApply(WSResponse::asJson)
                .thenApply(j -> {
                    if (j.get("ok").asBoolean())
                        return new Reply(j.has("result") && j.get("result").has("message_id")
                                ? j.get("result").get("message_id").asLong()
                                : 0);

                    return new Reply(j.get("description").asText());
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

    public static class Keyboard {
        private final List<List<Button>> buttons = new ArrayList<>(1);

        public Keyboard() {
            buttons.add(new ArrayList<>(1));
        }

        public Keyboard newLine() {
            buttons.add(new ArrayList<>(0));
            return this;
        }

        public Keyboard button(final Button... button) {
            Arrays.stream(button).filter(Objects::nonNull).forEach(b -> button(b, false));
            return this;
        }

        public void button(final Button button, final boolean newLine) {
            if (button == null)
                return;

            if (newLine)
                buttons.add(new ArrayList<>(1));

            buttons.get(buttons.size() - 1).add(button);
        }

        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            buttons.stream().filter(r -> !isEmpty(r)).forEach(row -> {
                final ArrayNode rowNode = node.withArray("inline_keyboard").addArray();
                row.forEach(b -> {
                    final ObjectNode bNode = rowNode.addObject();
                    bNode.put("text", b.text);
                    bNode.put("callback_data", b.data);
                });
            });

            return node;
        }
    }

    public static class Button {
        private final String text, data;

        public Button(final String text, final CommandType data) {
            this(text, data.toString());
        }

        public Button(final String text, final String data) {
            this.text = text;
            this.data = data;
        }
    }
}
