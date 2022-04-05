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
    CompletionStage<Reply> sendText(String body, ParseMode mode, Chat target, Keyboard kbd);

    CompletionStage<Reply> sendMedia(TFile media, String caption, ParseMode mode, Chat target, Keyboard kbd);

    CompletionStage<Reply> sendReaction(String reaction, boolean asAlert, long queryId, int showTime);

    CompletionStage<Reply> uploadMedia(RawMedia media, String caption, ParseMode mode, Chat target, Keyboard kbd);

    CompletionStage<Reply> editBody(String body, ParseMode mode, Chat target, long editMessageId);

    CompletionStage<Reply> editCaption(String caption, ParseMode mode, Chat target, long editMessageId);

    CompletionStage<Reply> editKeyboard(Keyboard kbd, Chat target, long editMessageId);

    CompletionStage<Reply> editMedia(TFile media, Chat target, long editMessageId);

    CompletionStage<Reply> dropMessage(Chat target, long messageId);

    enum ParseMode {Md2, Html;
        public String asText() {
            return this == Html ? "HTML" : "MarkdownV2";
        }
    }

    Keyboard helpKbd = new Keyboard().button(CommandType.contextHelp.b());
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
}
