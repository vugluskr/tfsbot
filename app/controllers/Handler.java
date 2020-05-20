package controllers;

import actors.protocol.WakeUpNeo;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import model.User;
import model.telegram.api.TeleFile;
import model.telegram.api.UpdateRef;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;
import scala.reflect.api.Names;
import services.HeadQuarters;
import services.UserService;
import utils.Strings;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    private UserService userService;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {
            if (request.hasBody()) {
                logger.debug("INCOMING:\n" + request.body().asJson());
                final UpdateRef updateRef = Json.fromJson(request.body().asJson(), UpdateRef.class);

                if (updateRef != null) {
                    final User user = userService.getUser(updateRef);

                    final long id = updateRef.getMessage() != null ? updateRef.getMessage().getMessageId() : 0;
                    final String text = updateRef.getMessage() != null ? updateRef.getMessage().getText() : null;
                    final String callback = updateRef.getCallback() != null ? updateRef.getCallback().getData() : null;
                    final long callbackId = callback == null ? 0 : updateRef.getCallback().getId();
                    final TeleFile file = updateRef.getMessage() != null ? updateRef.getMessage().getTeleFile() : null;

                    logger.debug("Input: {id: " + id + ", text: " + text + ", callback: " + callback + "}", id, text, callback);

                    CompletableFuture.runAsync(() -> hq.accept(user, file, file != null ? updateRef.getMessage().getCaption() : text, callback, id, callbackId));
                } else
                    logger.debug("No UpdateRef object in request body");
            } else
                logger.debug("Empty request body");
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }
}
