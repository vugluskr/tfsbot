package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.TFile;
import model.User;
import play.libs.Json;
import sql.TFileSystem;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings.Callback;
import utils.Strings.Uni;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static utils.LangMap.v;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class GUI {
    public interface Buttons {
        Button mkLabelButton = new Button(Uni.label, Callback.mkLabel);
        Button searchButton = new Button(Uni.search, Callback.search);
        Button mkDirButton = new Button(Uni.mkdir, Callback.mkDir);
        Button gearButton = new Button(Uni.gear, Callback.gear);
        Button cancelButton = new Button(Uni.cancel, Callback.cancel);
        Button goUpButton = new Button(Uni.updir, Callback.goUp);
        Button rewindButton = new Button(Uni.rewind, Callback.rewind);
        Button forwardButton = new Button(Uni.forward, Callback.forward);
        Button renameButton = new Button(Uni.rename, Callback.rename);
        Button putButton = new Button(Uni.put, Callback.put);
        Button moveButton = new Button(Uni.move, Callback.move);
        Button dropButton = new Button(Uni.drop, Callback.drop);
        Button checkAllButton = new Button(Uni.checkAll, Callback.checkAll);
        Button shareButton = new Button(Uni.share, Callback.share);
        Button mkLinkButton = new Button(Uni.Link, Callback.mkLink);
        Button mkGrantButton = new Button(Uni.Person, Callback.mkGrant);
        Button okButton = new Button(Uni.put, Callback.ok);
        Button voidButton = new Button(Uni.cancel, Callback.Void);
    }

    private static final Keyboard fileKbd = new Keyboard(), voidKbd = new Keyboard(), yesNoKbd = new Keyboard();

    static {
        fileKbd.button(Buttons.renameButton,
                Buttons.moveButton,
                Buttons.dropButton,
                Buttons.cancelButton);

        voidKbd.button(Buttons.voidButton);

        yesNoKbd.button(Buttons.okButton, Buttons.voidButton);
    }

    @Inject
    private TgApi tgApi;

    @Inject
    private TFileSystem fs;

    public void cleanup(final User user) {
        try {
            fs.selectServiceWindows(user.getId())
                    .forEach(msgId -> CompletableFuture.runAsync(() -> tgApi.deleteMessage(msgId, user.getId())));
        } finally {
            fs.deleteServiceWindows(user.getId());
        }
    }

    public void yesNoPrompt(final LangMap.Value question, final User user, final Object... args) {
        CompletableFuture.runAsync(() -> tgApi.sendMessage(TextUtils.escapeMd(v(question, user, args)), TgApi.formatMd2, user.getId(), yesNoKbd.toJson())
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.getId())));
    }

    public void dialog(final LangMap.Value text, final User user, final Object... args) {
        CompletableFuture.runAsync(() -> tgApi.sendMessage(TextUtils.escapeMd(v(text, user, args)), TgApi.formatMd2, user.getId(), voidKbd.toJson())
                .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.getId())));
    }


    public void makeFileDialog(final TFile entry, final User user) {
        CompletableFuture.runAsync(() ->
                tgApi.sendMedia(entry, entry.getPath(), fileKbd.toJson(), user.getId())
                        .thenAccept(reply -> fs.addServiceWin(reply.messageId, user.getId())));
    }

    public void sendBox(final FlowBox box, final User user) {
        CompletableFuture.runAsync(() -> {
            if (user.getLastMessageId() > 0)
                tgApi.editMessageText(user.getId(), user.getLastMessageId(), box.body.toString(), box.format, box.kbd.toJson())
                        .thenApply(reply -> {
                            if (reply.ok)
                                return true;

                            fs.addServiceWin(user.getLastMessageId(), user.getId());
                            user.setLastMessageId(0);
                            fs.updateLastMessageId(user.getLastMessageId(), user.getId());
                            return false;
                        })
                        .thenAccept(wasOk -> {
                            if (!wasOk)
                                tgApi.sendMessage(box.body.toString(), box.format, user.getId(), box.kbd.toJson())
                                .thenAccept(reply -> {
                                    if (reply.ok) {
                                        user.setLastMessageId(reply.messageId);
                                        fs.updateLastMessageId(user.getLastMessageId(), user.getId());
                                    }
                                }).toCompletableFuture().join();
                        });
            else
                tgApi.sendMessage(box.body.toString(), box.format, user.getId(), box.kbd.toJson())
                        .thenAccept(reply -> {
                            if (reply.ok) {
                                user.setLastMessageId(reply.messageId);
                                fs.updateLastMessageId(user.getLastMessageId(), user.getId());
                            }
                        });
        });
    }

    public static class Keyboard {
        private final List<List<Button>> buttons = new ArrayList<>(1);

        {
            buttons.add(new ArrayList<>(1));
        }

        public void newLine() {
            buttons.add(new ArrayList<>(0));
        }

        public void button(final Button... button) {
            Arrays.stream(button).filter(Objects::nonNull).forEach(b -> button(b, false));
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

        public Button(final String text, final Callback data) {
            this(text, data.toString());
        }

        public Button(final String text, final String data) {
            this.text = text;
            this.data = data;
        }
    }
}
