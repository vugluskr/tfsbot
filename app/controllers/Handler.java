package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import model.*;
import model.user.ShareGranter;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.TFileFactory;

import javax.inject.Inject;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class Handler extends Controller {
    private static final Logger.ALogger logger = Logger.of(Handler.class);


    @Inject
    private TgApi api;

    @Inject
    private UserService userService;

    @Inject
    private TfsService tfs;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {
            final JsonNode js;
            if (request.hasBody() && (js = request.body().asJson()) != null)
                CompletableFuture.runAsync(() -> handleJson(js))
                        .exceptionally(e -> {
                            logger.error(e.getMessage(), e);
                            return null;
                        });
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }

    private void handleJson(final JsonNode js) {
        final User user;

        if (js.has("callback_query")) {
            api.sendCallbackAnswer("", js.get("callback_query").get("id").asLong(), false, 0);
            final String cb = js.get("callback_query").get("data").asText();
            user = getUser(js.get("callback_query").get("from"));

            final int del = cb.indexOf(':');

            if (del < 1) {
                logger.debug("Неизвестный науке коллбек: " + cb);
                handleUserRequest(user, u -> {
                    userService.reset(user);
                    user.doView();
                }, js);
                return;
            }

            final Command command = new Command();
            command.elementIdx = del < cb.length() - 1 ? getInt(cb.substring(del + 1)) : -1;
            command.type = CommandType.ofString(cb);

            handleUserRequest(user, u -> u.onCallback(command), js);
        } else if (js.has("message")) {
            CompletableFuture.runAsync(() -> api.deleteMessage(js.get("message").get("message_id").asLong(), js.get("message").get("from").get("id").asLong()));

            final JsonNode msg = js.get("message");

            final String text = msg.has("text") ? msg.get("text").asText() : null;
            user = getUser(msg.get("from"));

            if (msg.has("forward_from") && user.getRole() instanceof ShareGranter) {
                final TFile file = new TFile();
                file.type = ContentType.CONTACT;
                final JsonNode c = msg.get("forward_from");
                file.setOwner(c.get("id").asLong());
                final String f = c.has("first_name") ? c.get("first_name").asText() : "";
                final String u = c.has("username") ? c.get("username").asText() : "";
                file.name = notNull(f, notNull(u, "u" + file.getOwner()));

                handleUserRequest(user, u0 -> u0.onFile(file), js);
            } else if (text != null) {
                if (text.equals("/start")) {
                    api.sendText("Welcome!", null, null, user.id);
                    handleUserRequest(user, User::doView, js);
                } else if (text.equals("/reset"))
                    handleUserRequest(user, this::doReset, js);
                else if (text.equals("/help"))
                    handleUserRequest(user, u -> api.dialogUnescaped(u.doHelp(), u, TgApi.voidKbd), js);
                else if (text.startsWith("/start shared-"))
                    handleUserRequest(user, u -> u.joinShare(notNull(text).substring(14)), js);
                else if (text.startsWith("/opds ")) {
                    final String[] parts = notNull(text).substring(5).trim().split("\\s");
                    if (parts.length == 2 && !isEmpty(parts[0]) && !isEmpty(parts[1]))
                        tfs.mk(TFileFactory.opdsDir(parts[1].trim(), parts[0].trim(), user.entryId(), user.id));

                    handleUserRequest(user, User::doView, js);
                } else if (text.startsWith("/fbs ")) {
                    final String q = notNull(text.substring(3));

                    api.sendText("<a href=\"/logme@telefsBot\">ссылка</a>, а тут просто /command", ParseMode.html, null, user.id);
                } else
                    handleUserRequest(user, u -> u.onInput(text), js);
            } else {
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
                    logger.debug("We've got document!\n" + msg);
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
                    file.uniqId = msg.has("file_unique_id") ? msg.get("file_unique_id").asText() : p;
                    file.refId = msg.has("file_id") ? msg.get("file_id").asText() : p;
                    file.name = notNull((notNull(f) + " " + notNull(l)), notNull(u, notNull(p, "u" + c.get("user_id").asText())));
                } else {
                    file.type = null;
                    logger.debug("Необслуживаемый тип сообщения");
                    return;
                }

                if (file.type != null && attachNode != null) {
                    if (file.refId == null) file.refId = attachNode.get("file_id").asText();
                    if (file.uniqId == null) file.uniqId = attachNode.get("file_unique_id").asText();

                    if (file.name == null)
                        file.name = msg.has("caption") && !msg.get("caption").asText().trim().isEmpty()
                                ? msg.get("caption").asText().trim()
                                : file.type.name().toLowerCase() + "_" + file.uniqId;

                    if (file.type == ContentType.CONTACT)
                        file.refId = attachNode.toString();

                    handleUserRequest(user, u -> u.onFile(file), js);
                } else
                    handleUserRequest(user, User::doView, js);
            }
        } else {
            logger.debug("Необслуживаемый тип сообщения");
        }
    }

    private void doReset(final User user) {
        final long userId = user.id;

        CompletableFuture.runAsync(() -> {
            api.cleanup(userId);
            if (user.lastMessageId > 0)
                api.deleteMessage(user.lastMessageId, userId);
            userService.reset(user);
            tfs.reinitUserTables(userId);
            tfs.dropUserShares(userId);
            user.doView();

            logger.info("User " + user.name + " #" + user.id + " rebuilded");
        }).exceptionally(e -> {
            logger.error("Resetting user #" + userId + ": " + e.getMessage(), e);
            return null;
        });
    }

    private void handleUserRequest(final User user, final Consumer<User> task, final JsonNode input) {
        CompletableFuture.runAsync(() -> {
            try {
                task.accept(user);
            } finally {
                userService.update(user);
            }
        }).exceptionally(e -> {
            logger.error("Handling input [" + input.toString() + "]: " + e.getMessage(), e);
            return null;
        });
    }

    private User getUser(final JsonNode node) {
        final String f = node.has("first_name") ? node.get("first_name").asText() : null;
        final String l = node.has("last_name") ? node.get("last_name").asText() : null;
        final String n = node.has("username") ? node.get("username").asText() : null;
        final long id = node.get("id").asLong();

        try {
            return userService.resolveUser(id,
                    node.has("language_code") ? node.get("language_code").asText() : "en",
                    notNull((notNull(f) + " " + notNull(l)), notNull(n, "u" + id)));
        } finally {
            api.cleanup(id);
        }
    }
}
