package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.CommandType;
import model.TFile;
import model.User;
import model.ContentType;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.HeadQuarters;
import services.TgApi;
import services.UserService;

import javax.inject.Inject;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.getInt;
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
    private TgApi api;

    @Inject
    private UserService userService;

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
        final Command command = new Command();

        if (js.has("callback_query")) {
            logger.debug("Callback:\n" + js);
            api.sendCallbackAnswer("", js.get("callback_query").get("id").asLong(), false, 0);
            final String cb = js.get("callback_query").get("data").asText();
            user = getUser(js.get("callback_query").get("from"));

            final int del = cb.indexOf(':');

            if (del < 1) {
                logger.debug("Неизвестный науке коллбек: " + cb);
                return;
            }

            command.elementIdx = del < cb.length() - 1 ? getInt(cb.substring(del + 1)) : -1;
            command.type = CommandType.ofString(cb);

            if (command.type == CommandType.Void)
                return; // просто закрыли диалоги
        } else if (js.has("message")) {
            CompletableFuture.runAsync(() -> api.deleteMessage(js.get("message").get("message_id").asLong(), js.get("message").get("from").get("id").asLong()));

            final JsonNode msg = js.get("message");
            final String text = msg.has("text") ? msg.get("text").asText() : null;
            user = getUser(msg.get("from"));

            if (text != null) {
                if (text.equals("/start") || text.equals("/reset"))
                    command.type = CommandType.resetToRoot;
                else if (text.equals("/help"))
                    command.type = CommandType.contextHelp;
                else if (text.startsWith("/start shared-")) {
                    command.input = notNull(text).substring(14);
                    command.type = CommandType.joinPublicShare;
                } else {
                    command.input = text;

                    if (user.isWaitDirInput())
                        command.type = CommandType.mkDir;
                    else if (user.isWaitLabelInput())
                        command.type = CommandType.mkLabel;
                    else if (user.isWaitRenameDirInput())
                        command.type = CommandType.renameDir;
                    else if (user.isWaitRenameFileInput())
                        command.type = CommandType.renameFile;
                    else if (user.isWaitEditLabelInput())
                        command.type = CommandType.editLabel;
                    else if (user.isWaitPasswordInput())
                        command.type = CommandType.lock;
                    else if (user.isWaitUnlockDirInput())
                        command.type = CommandType.unlockDir;
                    else if (user.isWaitUnlockFileInput())
                        command.type = CommandType.unlockFile;
                    else
                        command.type = CommandType.doSearch;

                    user.resetInputWait();
                }
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

                    command.type = user.isWaitFileGranting() && file.type == ContentType.CONTACT ? CommandType.grantAccess : CommandType.uploadFile;
                    if (!user.isWaitFileGranting() && file.type == ContentType.CONTACT)
                        file.refId = attachNode.toString();

                    command.file = file;
                }
            }
        } else {
            logger.debug("Необслуживаемый тип сообщения");
            return;
        }

        CompletableFuture.runAsync(() -> hq.doCommand(command, user))
                .exceptionally(e -> {
                    logger.error("Handling command [" + command + "]: " + e.getMessage(), e);
                    return null;
                });
    }

    private User getUser(final JsonNode node) {
        final String f = node.has("first_name") ? node.get("first_name").asText() : null;
        final String l = node.has("last_name") ? node.get("last_name").asText() : null;
        final String n = node.has("username") ? node.get("username").asText() : null;

        final User user = new User();
        user.setId(node.get("id").asLong());
        user.setLang(node.has("language_code") ? node.get("language_code").asText() : "en");
        user.name = notNull((notNull(f) + " " + notNull(l)), notNull(n, "u" + user.getId()));

        try {
            return userService.resolveUser(user);
        } finally {
            api.cleanup(user.getId());
        }
    }
}
