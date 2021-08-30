package modules;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.Command;
import model.CommandType;
import model.MediaMessage;
import model.User;
import play.Logger;
import play.api.libs.json.Json;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import services.HandlerService;
import services.TgApi;
import services.UserService;
import sql.MediaMessageMapper;
import sql.UserMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static utils.TextUtils.getInt;
import static utils.TextUtils.notNull;

@Singleton
public class Jobs {
    private static final Logger.ALogger logger = Logger.of(Jobs.class);
    @Inject
    private  ActorSystem actorSystem;
    @Inject
    private  ExecutionContext executionContext;
    @Inject
    private TgApi api;
    @Inject
    private HandlerService handlerService;

    @Inject
    private MediaMessageMapper mediaMessageMapper;
    @Inject
    private UserService userService;


    @Inject
    private  Config  config;
    private volatile AtomicReference<Long> updateId= new AtomicReference<>(0L);


    @Inject
    private  void init() {
        actorSystem.scheduler().schedule(
                Duration.create(10, TimeUnit.SECONDS),// delay
                Duration.create(500, TimeUnit.MILLISECONDS), // interval
                this::pool,
                executionContext
        );
      actorSystem.scheduler().schedule(
                Duration.create(1, TimeUnit.SECONDS),// delay
                Duration.create(60, TimeUnit.SECONDS), // interval
                this::clearMessage,
                executionContext
        );
    }

    private void pool() {
        try {
            api.getUpdates(updateId.get()).thenApplyAsync(updates -> {
                if (updates != null && updates.size() > 0)
                    for (JsonNode js : updates) {
                        updateId.set(js.get("update_id").asLong() + 1);
                        handlerService.handleJson(js);
                    }
                return null;
            }).toCompletableFuture().get();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private  void clearMessage(){
          LocalDateTime timeToDelete = LocalDateTime.now().minusSeconds(config.getInt("service.bot.delete_media_after_sec"));
          List<MediaMessage> lst = mediaMessageMapper.selectMessagesByTime(timeToDelete);
          if(lst.size()>0) {
              lst.forEach(mediaMessage -> {
                  api.deleteMessage(mediaMessage.getMessageId(), mediaMessage.getUserId());
                  Command command = new Command();
                  command.elementIdx = -1;
                  command.type = CommandType.ofString("0:");
                  JsonNode jsonNode = new ObjectMapper().createObjectNode();
                  User user = userService.resolveUser(mediaMessage.getUserId(), "en", "");
                  handlerService.handleUserRequest(user, u -> u.onCallback(command), jsonNode);
                  mediaMessageMapper.deleteMessages(timeToDelete);
              });
          }
    }
}
