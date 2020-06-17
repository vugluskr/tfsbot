package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import model.CommandType;
import model.TFile;
import model.User;
import model.ContentType;
import model.ParseMode;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import sql.TFileSystem;
import utils.LangMap;
import utils.TextUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static utils.LangMap.v;
import static utils.TextUtils.isEmpty;

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

    @Inject
    public TgApi(final Config config, final WSClient ws, final TFileSystem fs) {
        this.ws = ws;
        this.fs = fs;
        apiUrl = config.getString("service.bot.api_url");
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
        CompletableFuture.runAsync(() -> sendText(TextUtils.escapeMd(v(text, user, args)), ParseMode.md2, kbd.toJson(), user.getId())
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.getId())));
    }

    public void dialogUnescaped(final LangMap.Value text, final User user, final Keyboard kbd, final Object... args) {
        CompletableFuture.runAsync(() -> sendText(v(text, user, args), ParseMode.md2, kbd.toJson(), user.getId())
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.getId())));
    }

    public void sendContent(final TFile file, final String body, final String format, final Keyboard keyboard, final User user) {
        sendContent(file, body, format, keyboard, user, 0);
    }

    private void sendContent(final TFile file, final String body, final String format, final Keyboard keyboard, final User user, final int cnt) {
        final Function<Throwable, Void> fuckup = e -> {
            logger.error("[#" + cnt + "] " + e.getMessage(), e);
            return null;
        };

        if ((user.getLastRefId() != null && file == null) || (user.getLastRefId() == null && file != null)) {
            final long toDel = user.getLastMessageId();
            CompletableFuture.runAsync(() -> deleteMessage(toDel, user.getId()));
            user.setLastMessageId(0);
        }

        final String ks = keyboard == null ? null : keyboard.toJson().toString();

        if (user.getLastMessageId() > 0) {
            final boolean sameKbd = Objects.equals(ks, user.getLastKeyboard() == null ? null : user.getLastKeyboard().toJson().toString());
            final boolean sameFile = Objects.equals(file == null ? null : file.getRefId(), user.getLastRefId());
            final boolean sameText = Objects.equals(body, user.getLastText());

            if (sameFile && sameText && sameKbd)
                return;

            final Consumer<Reply> editSuccessConsumer = reply -> {
                if (!reply.ok) {
                    deleteMessage(user.getLastMessageId(), user.getId());
                    user.setLastMessageId(0);
                    fs.updateLastMessageId(0, user.getId());
                    if (cnt < 2)
                        sendContent(file, body, format, keyboard, user, cnt + 1);
                }
            };

            if (file != null) {
                if (sameFile) {
                    if (sameText)
                        CompletableFuture.runAsync(() ->
                                editKeyboard(keyboard == null ? Json.newObject() : keyboard.toJson(), user.getId(), user.getLastMessageId())
                                        .thenAccept(editSuccessConsumer)
                                        .exceptionally(fuckup));
                    else
                        CompletableFuture.runAsync(() ->
                                editCaption(body, format, keyboard == null ? null : keyboard.toJson(), user.getId(), user.getLastMessageId())
                                        .thenAccept(editSuccessConsumer)
                                        .exceptionally(fuckup));
                } else
                    CompletableFuture.runAsync(() ->
                            editMedia(file.getRefId(), file.getType(), keyboard == null ? null : keyboard.toJson(), user.getId(), user.getLastMessageId())
                                    .thenAccept(editSuccessConsumer)
                                    .exceptionally(fuckup));
            } else {
                if (!sameKbd && sameText)
                    CompletableFuture.runAsync(() ->
                            editKeyboard(keyboard == null ? Json.newObject() : keyboard.toJson(), user.getId(), user.getLastMessageId())
                                    .thenAccept(editSuccessConsumer)
                                    .exceptionally(fuckup));
                else
                    CompletableFuture.runAsync(() ->
                            editText(body, format, keyboard == null ? null : keyboard.toJson(), user.getId(), user.getLastMessageId())
                                    .thenAccept(editSuccessConsumer)
                                    .exceptionally(fuckup));
            }
        } else {
            final Consumer<TgApi.Reply> sendSuccessConsumer = reply -> {
                if (reply.ok) {
                    user.setLastMessageId(reply.messageId);
                    fs.updateLastMessageId(reply.messageId, user.getId());
                } else
                    logger.error("Cant send content: " + reply.desc);
            };

            if (file != null)
                CompletableFuture.runAsync(() ->
                        sendMedia(file.getRefId(), file.getType(), body, format, keyboard == null ? null : keyboard.toJson(), user.getId())
                                .thenAccept(sendSuccessConsumer)
                                .exceptionally(fuckup));
            else
                CompletableFuture.runAsync(() ->
                        sendText(body, format, keyboard == null ? null : keyboard.toJson(), user.getId())
                                .thenAccept(sendSuccessConsumer)
                                .exceptionally(fuckup));
        }

        user.setLastRefId(file == null ? null : file.getRefId());
        user.setLastKeyboard(ks);
        user.setLastText(body);
    }

    public void deleteMessage(final long messageId, final long userId) {
        if (messageId > 0)
            CompletableFuture.runAsync(() -> ws.url(apiUrl + "deleteMessage").setContentType("application/json").post("{\"chat_id\":" + userId + ",\"message_id\":" + messageId + "}"));
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

    public void sendCallbackAnswer(final String text, final long callbackId, final boolean alert, final int cacheTime) {
        final ObjectNode node = Json.newObject();
        node.put("callback_query_id", callbackId);
        node.put("text", text);
        node.put("show_alert", alert);
        node.put("cache_time", cacheTime);

        CompletableFuture.runAsync(() -> ws.url(apiUrl + "answerCallbackQuery").post(node));
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

        return doCall(node, type.getUrlPath());
    }

    private CompletionStage<Reply> doCall(final JsonNode node, final String partialUrl) {
        return ws.url(apiUrl + partialUrl)
                .post(node)
                .thenApply(wsr -> {
                    final JsonNode j = wsr.asJson();

                    if (j.get("ok").asBoolean())
                        return new Reply(j.has("result") && j.get("result").has("message_id")
                                ? j.get("result").get("message_id").asLong()
                                : 0);

                    logger.debug("On request [" + apiUrl + partialUrl + "]:\n" + node.toString() + "\ngot response:\n" + j.toString());

                    return new Reply(j.get("description").asText());
                })
                .exceptionally(e -> {
                    logger.error("On request [" + apiUrl + partialUrl + "]:\n" + node.toString() + "\ngot error: " + e.getMessage(), e);
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

    public static class Keyboard {
        private final List<List<Button>> buttons = new ArrayList<>(1);

        public Keyboard() {
            buttons.add(new ArrayList<>(1));
        }

        public void newLine() {
            buttons.add(new ArrayList<>(0));
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

        public static Keyboard fromJson(final String json) {
            final JsonNode node = Json.parse(json);
            final Keyboard k = new Keyboard();

            for (int i = 0; i < node.get("inline_keyboard").size(); i++) {
                final JsonNode bn = node.get("inline_keyboard").get(i);

                for (int j = 0; j < bn.size(); j++) {
                    k.button(new Button(bn.get(j).get("text").asText(), bn.get(j).get("callback_data").asText()));

                    if (i < node.get("inline_keyboard").size() - 1)
                        k.newLine();
                }
            }

            return k;
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
