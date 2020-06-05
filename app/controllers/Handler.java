package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.GUI;
import services.HeadQuarters;
import services.TgApi;
import services.UserService;
import utils.Strings;

import javax.inject.Inject;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class Handler extends Controller {
    private static final Logger.ALogger logger = Logger.of(Handler.class);

    @Inject
    private HeadQuarters hq;

    @Inject
    private TgApi tgApi;

    @Inject
    private GUI gui;

    @Inject
    private UserService userService;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {
            final JsonNode js;
            if (request.hasBody() && (js = request.body().asJson()) != null) {
                if (js.has("callback_query")) {
                    CompletableFuture.runAsync(() -> {
                        tgApi.sendCallbackAnswer("", js.get("callback_query").get("id").asLong(), false, 0);
                        final String cb = js.get("callback_query").get("data").asText();
                        final User preUser = preUser(js.get("callback_query").get("from"));

                        if (cb.equals(Strings.Callback.Void.name())) // close dialogs
                            return;

                        CompletableFuture.runAsync(() -> hq.callback(cb, userService.resolveUser(preUser)))
                                .exceptionally(e -> {
                                    logger.error("Callback: " + e.getMessage(), e);
                                    return null;
                                });
                    });
                } else if (js.has("message")) {
                    CompletableFuture.runAsync(() -> tgApi.deleteMessage(js.get("message").get("message_id").asLong(), js.get("message").get("from").get("id").asLong()));

                    final JsonNode msg = js.get("message");
                    final String text = msg.has("text") ? msg.get("text").asText() : null;

                    if (text != null)
                        CompletableFuture.runAsync(() -> hq.text(text, userService.resolveUser(preUser(msg.get("from")))))
                                .exceptionally(e -> {
                                    logger.error("Text: " + e.getMessage(), e);
                                    return null;
                                });
                    else {
                        final JsonNode attachNode;
                        final TFile file = new TFile();

                        if (msg.has("photo") && msg.get("photo").size() > 0) {
                            if (msg.get("photo").size() == 1)
                                attachNode = msg.get("photo").get(0);
                            else {
                                final TreeMap<Long, JsonNode> map = new TreeMap<>();

                                for (int i = 0; i < msg.get("photo").size(); i++)
                                    map.put(msg.get("photo").get(i).get("file_size").asLong(), msg.get("photo").get(i));

                                attachNode = map.lastEntry().getValue();
                            }
                            file.type = ContentType.PHOTO;
                        } else if (msg.has("video")) {
                            attachNode = msg.get("video");
                            file.type = ContentType.VIDEO;
                        } else if (msg.has("document")) {
                            attachNode = msg.get("document");
                            file.name = attachNode.get("file_name").asText();
                            file.type = ContentType.DOCUMENT;
                        } else if (msg.has("audio")) {
                            attachNode = msg.get("audio");
                            file.type = ContentType.AUDIO;
                        } else if (msg.has("voice")) {
                            attachNode = msg.get("voice");
                            file.type = ContentType.VOICE;
                        } else if (msg.has("sticker")) {
                            attachNode = msg.get("sticker");
                            file.type = ContentType.STICKER;
                        } else if (msg.has("contact")) {
                            attachNode = msg.get("contact");
                            file.type = ContentType.CONTACT;

                            // dirty simple hack :)
                            final JsonNode c = msg.get("contact");
                            file.setOwner(c.get("user_id").asLong());
                            final String f = c.has("first_name") ? c.get("first_name").asText() : "";
                            final String l = c.has("last_name") ? c.get("last_name").asText() : "";
                            final String u = c.has("username") ? c.get("username").asText() : "";
                            final String p = c.has("phone_number") ? c.get("phone_number").asText() : "";
                            file.uniqId = file.refId = p;
                            file.name = notNull((notNull(f) + " " + notNull(l)), notNull(u, notNull(p, "u" + c.get("user_id").asText())));
                        } else {
                            file.type = null;
                            attachNode = null;
                            logger.debug("Необслуживаемый тип сообщения");
                        }

                        if (file.type != null && attachNode != null) {
                            if (file.refId == null) file.refId = attachNode.get("file_id").asText();
                            if (file.uniqId == null) file.uniqId = attachNode.get("file_unique_id").asText();

                            if (file.name == null)
                                file.name = msg.has("caption") && !msg.get("caption").asText().trim().isEmpty()
                                        ? msg.get("caption").asText().trim()
                                        : file.type.name().toLowerCase() + "_" + file.uniqId;

                            CompletableFuture.runAsync(() -> hq.file(file, userService.resolveUser(preUser(msg.get("from")))))
                                    .exceptionally(e -> {
                                        logger.error("File: " + e.getMessage(), e);
                                        return null;
                                    });
                        }
                    }
                } else
                    logger.debug("Необслуживаемый тип сообщения");
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }

    private User preUser(final JsonNode node) {
        final String f = node.has("first_name") ? node.get("first_name").asText() : null;
        final String l = node.has("last_name") ? node.get("last_name").asText() : null;
        final String n = node.has("username") ? node.get("username").asText() : null;

        final User user = new User();
        user.setId(node.get("id").asLong());
        user.setLang(node.has("language_code") ? node.get("language_code").asText() : "en");
        user.name = notNull((notNull(f) + " " + notNull(l)), notNull(n, "u" + user.getId()));

        try {
            return user;
        } finally {
            gui.cleanup(user);
        }
    }
}
