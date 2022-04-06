package services;

import akka.protobuf.ByteString;
import akka.stream.IOResult;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import model.CommandType;
import model.ContentType;
import model.TFile;
import play.libs.Json;
import play.libs.ws.WSResponse;
import services.impl.BotApiImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 13:31
 * tfs ☭ sweat and blood
 */
@ImplementedBy(BotApiImpl.class)
public interface BotApi {
    CompletionStage<Reply> sendText(TextMessage message);

    CompletionStage<Reply> sendMedia(MediaMessage message);

    CompletionStage<Boolean> editText(TextMessage message, long editMessageId);

    CompletionStage<Boolean> editMedia(MediaMessage message, long editMessageId);

    CompletionStage<Void> sendReaction(ReactionMessage message);

    CompletionStage<Void> dropMessage(long messageId, Chat target);

    default CompletionStage<Void> dropMessage(long messageId, long userId) {
        return dropMessage(messageId, Chat.of(userId));
    }

    default CompletionStage<Void> dropMessage(long messageId, String channelId) {
        return dropMessage(messageId, Chat.of(channelId));
    }

    enum ParseMode {Md2, Html;
        public String asText() {
            return this == Html ? "HTML" : "MarkdownV2";
        }
    }

    Keyboard helpKbd = new Keyboard().button(CommandType.justCloseCmd.b());
    Keyboard voidKbd = new Keyboard().button(CommandType.Void.b());
    Keyboard yesNoKbd = new Keyboard().button(CommandType.confirm.b()).button(CommandType.cancel.b());

    class RawMedia {
        public Source<ByteString, CompletionStage<IOResult>> src;
        public String filename;
        public String mimeType;
        public ContentType type;

        public boolean isComplete() {
            return src != null && !isEmpty(filename) && !isEmpty(mimeType) && type != null && ContentType.media.contains(type) && type != ContentType.CONTACT;
        }
    }

    class Chat {
        final String channelId;
        final long userId;

        private Chat(final String channelId) {
            this.channelId = channelId;
            this.userId = -1;
        }

        private Chat(final long userId) {
            this.userId = userId;
            this.channelId = null;
        }

        @Override
        public String toString() {
            return userId > 0 ? String.valueOf(userId) : channelId;
        }

        public static Chat of(final String channelId) {
            if (isEmpty(channelId))
                return null;

            return new Chat((notNull(channelId).charAt(0) == '@' ? "" : "@") + notNull(channelId));
        }

        public static Chat of(final long userId) {
            if (userId <= 0)
                return null;

            return new Chat(userId);
        }

        public void set(final String field, final ObjectNode node) {
            if (userId > 0)
                node.put(field, userId);
            else if (channelId != null)
                node.put(field, channelId);
        }
    }

    class Reply {
        private final boolean ok;
        private long msgId;
        private String desc;
        private String rawData;

        public Reply(final boolean ok) {
            this.ok = ok;
        }

        public Reply(final WSResponse wsr) {
            if (wsr == null || isEmpty((rawData = wsr.getBody()))) {
                this.ok = false;
                this.desc = "Reply is null";
            } else {
                final JsonNode js = wsr.asJson();

                if (js == null) {
                    this.ok = false;
                    this.desc = "Reply is not a json";
                } else {
                    this.ok = js.has("ok") && js.get("ok").asBoolean();

                    if (ok) {
                        if (js.has("result") && js.get("result").has("message_id"))
                            msgId = js.get("result").get("message_id").asLong();
                    } else {
                        if (js.has("description"))
                            desc = js.get("description").asText();
                    }
                }
            }
        }

        public boolean isOk() {
            return ok;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(final String desc) {
            this.desc = desc;
        }

        public String getRawData() {
            return rawData;
        }

        public void setRawData(final String rawData) {
            this.rawData = rawData;
        }

        public long getMsgId() {
            return msgId;
        }

        public void setMsgId(final long msgId) {
            this.msgId = msgId;
        }

        public Reply withDesc(final String desc) {
            this.desc = desc;
            return this;
        }
    }

    class Keyboard {
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

    class Button {
        private final String text, data;

        public Button(final String text, final CommandType data) {
            this(text, data.toString());
        }

        public Button(final String text, final String data) {
            this.text = text;
            this.data = data;
        }
    }

    class ReactionMessage {
        public final Chat chat;
        public final String text;
        public final boolean alert;
        public final long queryId;

        public ReactionMessage(final long queryId, final String text, final boolean alert, final Chat chat) {
            this.chat = chat;
            this.text = text;
            this.alert = alert;
            this.queryId = queryId;
        }

        public ReactionMessage(final long queryId, final String text, final boolean alert, final long userId) {
            this(queryId, text, alert, Chat.of(userId));
        }

        public ReactionMessage(final long queryId, final String text, final boolean alert, final String channelId) {
            this(queryId, text, alert, Chat.of(channelId));
        }

        public ReactionMessage(final long queryId, final String text, final Chat chat) {
            this(queryId, text, false, chat);
        }

        public ReactionMessage(final long queryId, final String text, final long userId) {
            this(queryId, text, false, Chat.of(userId));
        }

        public ReactionMessage(final long queryId, final String text, final String channelId) {
            this(queryId, text, false, Chat.of(channelId));
        }
    }

    class AMessage {
        public final ParseMode mode;
        public final Keyboard kbd;
        public final Chat chat;

        public AMessage(final ParseMode mode, final Keyboard kbd, final Chat chat) {
            this.mode = mode;
            this.kbd = kbd;
            this.chat = chat;
        }

        public AMessage(final ParseMode mode, final Keyboard kbd, final String channelId) {
            this(mode, kbd, Chat.of(channelId));
        }

        public AMessage(final ParseMode mode, final Keyboard kbd, final long userId) {
            this(mode, kbd, Chat.of(userId));
        }
    }

    class TextMessage extends AMessage {
        public final String body;

        public TextMessage(final String body, final ParseMode mode, final Keyboard kbd, final Chat chat) {
            super(mode, kbd, chat);
            this.body = body;
        }

        public TextMessage(final String body, final ParseMode mode, final Keyboard kbd, final long userId) {
            super(mode, kbd, userId);
            this.body = body;
        }

        public TextMessage(final String body, final ParseMode mode, final Keyboard kbd, final String channelId) {
            super(mode, kbd, channelId);
            this.body = body;
        }
    }

    class MediaMessage extends AMessage {
        public final TFile media;
        public final RawMedia rawMedia;
        public final String caption;

        public MediaMessage(final TFile media, final RawMedia rawMedia, final String caption, final ParseMode mode, final Keyboard kbd, final Chat chat) {
            super(mode, kbd, chat);
            this.media = media;
            this.rawMedia = rawMedia;
            this.caption = caption;
        }

        public MediaMessage(final TFile media, final RawMedia rawMedia, final String caption, final ParseMode mode, final Keyboard kbd, final long userId) {
            super(mode, kbd, userId);
            this.media = media;
            this.rawMedia = rawMedia;
            this.caption = caption;
        }

        public MediaMessage(final TFile media, final RawMedia rawMedia, final String caption, final ParseMode mode, final Keyboard kbd, final String channelId) {
            super(mode, kbd, channelId);
            this.media = media;
            this.rawMedia = rawMedia;
            this.caption = caption;
        }

        public MediaMessage(final TFile media, final String caption, final ParseMode mode, final Keyboard kbd, final Chat chat) {
            this(media, null, caption, mode, kbd, chat);
        }

        public MediaMessage(final String caption, final ParseMode mode, final Keyboard kbd, final Chat chat) {
            this(null, null, caption, mode, kbd, chat);
        }

        public MediaMessage(final TFile media, final String caption, final ParseMode mode, final Keyboard kbd, final long userId) {
            this(media, null, caption, mode, kbd, userId);
        }

        public MediaMessage(final String caption, final ParseMode mode, final Keyboard kbd, final long userId) {
            this(null, null, caption, mode, kbd, userId);
        }

        public MediaMessage(final TFile media, final String caption, final ParseMode mode, final Keyboard kbd, final String channelId) {
            this(media, null, caption, mode, kbd, channelId);
        }

        public MediaMessage(final String caption, final ParseMode mode, final Keyboard kbd, final String channelId) {
            this(null, null, caption, mode, kbd, channelId);
        }
    }
}
