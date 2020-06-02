package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import model.Callback;
import model.telegram.api.ContactRef;
import model.telegram.api.TeleFile;
import model.telegram.api.UpdateRef;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.HeadQuarters;
import services.MemStore;
import utils.Strings;
import utils.TextUtils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

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
    private MemStore memStore;

    public Result get() {
        return ok();
    }

    public Result post(final Http.Request request) {
        try {
//            final JsonNode;
            if (request.hasBody()) {


                logger.debug("INCOMING:\n" + Json.stringify(request.body().asJson()));
                final UpdateRef update = Json.fromJson(request.body().asJson(), UpdateRef.class);

                if (update != null) {
                    final ContactRef cr;
                    if (update.getMessage() != null)
                        cr = update.getMessage().getContactRef();
                    else if (update.getCallback() != null)
                        cr = update.getCallback().getFrom();
                    else
                        cr = update.getEditedMessage().getContactRef();

                    final long id = update.getMessage() != null ? update.getMessage().getMessageId() : 0;
                    final String text = update.getMessage() != null ? update.getMessage().getText() : null;
                    final String callback = update.getCallback() != null ? TextUtils.notNull(update.getCallback().getData()) : null;
                    final long callbackId = callback == null ? 0 : update.getCallback().getId();
                    final TeleFile file = update.getMessage() != null ? update.getMessage().getTeleFile() : null;

                    final AtomicReference<Callback> cbRef = new AtomicReference<>(null);
                    if (!isEmpty(callback))
                        try {
                            final Strings.Callback cbs = Strings.Callback.ofString(callback);
                            if (cbs != null) {
                                cbRef.set(new Callback(cbs, callbackId, callback.indexOf(':') < callback.length() - 1 ? getInt(callback.substring(callback.indexOf(':') + 1)) : -1));
                            }
                        } catch (final Exception ignore) {
                            logger.debug(ignore.getMessage(), ignore);
                        }

                    CompletableFuture.runAsync(() -> memStore.getUser(cr).thenAccept(user ->
                            hq.accept(user, file, file != null ? update.getMessage().getCaption() : text, cbRef.get(), id))
                                              .exceptionally(throwable -> {
                                                  logger.error(throwable.getMessage(), throwable);
                                                  return null;
                                              }));
                }
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ok();
    }
}
