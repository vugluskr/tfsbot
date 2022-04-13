package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import model.request.CallbackRequest;
import model.request.TgRequest;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.BotApi;
import services.Router;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class Handler extends Controller {
    private static final Logger.ALogger logger = Logger.of(Handler.class);

    @Inject
    private BotApi api;

    @Inject
    private Router router;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {logger.info(">> " + request.body().asJson());} catch (final Exception ignore) {}
        try {
            final JsonNode js;
            final TgRequest r = request.hasBody() && ((js = request.body().asJson()) != null) ? TgRequest.resolve(js) : null;
            CompletableFuture.runAsync(() -> {
                try {
                    final JsonNode msg = request.body().asJson().get("message");
                    api.dropMessage(msg.get("message_id").asLong(), msg.get("from").get("id").asLong());
                } catch (final Exception ignore) {}
            });

            if (r != null)
                CompletableFuture.runAsync(() -> router.handle(r));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }
}
