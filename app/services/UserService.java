package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import model.User;
import model.user.*;
import play.Logger;
import play.libs.Json;
import sql.UserMapper;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.06.2020
 * tfs ☭ sweat and blood
 */
public class UserService {
    private static final Logger.ALogger logger = Logger.of(UserService.class);
    private static final Map<String, Constructor<? extends Role>> constructors;

    static {
        final Map<String, Constructor<? extends Role>> map = new HashMap<>();
        try {
            map.put(DirGearer.class.getName(), DirGearer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(DirMaker.class.getName(), DirMaker.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(DirViewer.class.getName(), DirViewer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(FileViewer.class.getName(), FileViewer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(LabelEditor.class.getName(), LabelEditor.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(LabelMaker.class.getName(), LabelMaker.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(LabelViewer.class.getName(), LabelViewer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(Locker.class.getName(), Locker.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(Renamer.class.getName(), Renamer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(Searcher.class.getName(), Searcher.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(ShareGranter.class.getName(), ShareGranter.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(Sharer.class.getName(), Sharer.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
            map.put(Unlocker.class.getName(), Unlocker.class.getDeclaredConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        constructors = Collections.unmodifiableMap(map);
    }

    @Inject
    private TfsService tfsService;

    @Inject
    private TgApi api;

    @Inject
    private UserMapper userMapper;

    @Inject
    private Config config;

    public String getBotName() {
        return config.getString("service.bot.nick");
    }

    public User resolveUser(final long id, final String lang, final String name) {
        Map<String, Object> map = null;
        try {
            map = userMapper.getUser(id);
        } catch (final Exception e) {
            logger.info(e.getMessage());
        }

        if (map == null) {
            logger.info("New user added: " + name + " #" + id);
            userMapper.insertUser(id, tfsService.initUserTables(id));

            map = userMapper.getUser(id);
        }

        if (!map.containsKey("data"))
            return new User(id, (UUID) map.get("root_id"), notNull(lang, "en"), name, null, null, null, 0, new DirViewer(api, tfsService, this, Json.newObject()));

        final JsonNode data = Json.parse((String) map.get("data"));

        try {
            return new User(
                    id, (UUID) map.get("root_id"),
                    lang, name,
                    (String) map.get("last_ref_id"),
                    (String) map.get("last_text"),
                    (String) map.get("last_kbd"),
                    (Long) map.get("last_message_id"),
                    constructors.getOrDefault(String.valueOf(data.get("_class").asText()), constructors.get(DirViewer.class.getName())).newInstance(api, tfsService, this, data));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);

            return null;
        }
    }

    public void update(final User user) {
        userMapper.updateUser(
                notNull(user.lastRefId),
                notNull(user.lastText),
                notNull(user.lastKeyboard),
                user.dump().toString(),
                user.id);
    }

    public <T extends Role> T morphTo(final Class<T> target, final User source) {
        try {
            final T role = target.getConstructor(TgApi.class, TfsService.class, UserService.class, JsonNode.class).newInstance(api, tfsService, this, source.dump());
            source.setRole(role);
            return role;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public void reset(final User user) {
        user.setRole(new DirViewer(api, tfsService, this, Json.parse("{\"entryId\":\""+user.rootId+"\"}")));
        user.lastMessageId = 0;
        user.lastKeyboard = null;
        user.lastText = null;
        user.lastRefId = null;
        update(user);
    }
}
