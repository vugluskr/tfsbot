package modules;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import model.user.FileViewer;
import play.Logger;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import services.HandlerService;
import services.TgApi;
import services.UserService;
import sql.MediaMessageMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class Jobs {
    private static final Logger.ALogger logger = Logger.of(Jobs.class);

    @Inject
    private ActorSystem actorSystem;

    @Inject
    private ExecutionContext executionContext;

    @Inject
    private TgApi api;

    @Inject
    private HandlerService handlerService;

    @Inject
    private MediaMessageMapper mediaMessageMapper;

    @Inject
    private UserService userService;

    private final AtomicLong updateId = new AtomicLong(0); // атомик уже кагбе "волатильный", в нём гарантирован доступ "по одному"

    private final int mediaDropDelay;

    @Inject
    public Jobs(final Config config) {
        mediaDropDelay = config.getInt("service.bot.delete_media_after_sec");
    }


    @Inject
    private void init() {
/* это нужно только для приватного запуска бота

        actorSystem.scheduler().schedule(
                Duration.create(10, TimeUnit.SECONDS),// delay
                Duration.create(500, TimeUnit.MILLISECONDS), // interval
                this::pool,
                executionContext
                                        );
*/

        actorSystem.scheduler().schedule(
                Duration.create(1, TimeUnit.SECONDS),// delay
                Duration.create(60, TimeUnit.SECONDS), // interval
                this::clearMessage,
                executionContext
                                        );
    }

    private void pool() {
        try {
            api.getUpdates(updateId.get())
                    .thenAccept(updates -> {
                        for (JsonNode js : updates) {
                            updateId.set(js.get("update_id").asLong() + 1);
                            handlerService.handleJson(js);
                        }
                    }).toCompletableFuture().get();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void clearMessage() {
        final LocalDateTime timeToDelete = LocalDateTime.now().minusSeconds(mediaDropDelay);

        CompletableFuture.runAsync(() ->
                        mediaMessageMapper.selectMessagesByTime(timeToDelete)
                                .forEach(msg -> {
                                    CompletableFuture.runAsync(() -> api.deleteMessage(msg.getMessageId(), msg.getUserId()));

                                    userService.morphTo(FileViewer.class, userService.resolveUser(msg.getUserId(), "en", "")).backToParent();
                                }))
                .thenAccept(unused -> mediaMessageMapper.deleteMessages(timeToDelete));
    }
}
