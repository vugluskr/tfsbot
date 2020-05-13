package controllers;

import model.TFile;
import model.User;
import model.telegram.api.MessageRef;
import model.telegram.api.TeleFile;
import model.telegram.api.UpdateRef;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.CmdService;
import services.FsService;
import services.GuiService;
import services.UserService;
import utils.UOpts;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class Handler extends Controller {
    private static final Logger.ALogger logger = Logger.of(Handler.class);

    @Inject
    private CmdService cmdService;

    @Inject
    private UserService userService;

    @Inject
    private FsService fsService;

    @Inject
    private GuiService guiService;

    public Result handle(final Http.Request request) {
        if (!request.hasBody() || !request.method().equalsIgnoreCase("POST"))
            return ok();

        UpdateRef input = null;
        try { input = Json.fromJson(request.body().asJson(), UpdateRef.class); } catch (final Exception ignore) { }

        if (input == null) {
            logger.debug("No input request: " + request.uri());
            return ok();
        }

        logger.debug("INCOMING MESSAGE:\n" + request.body().asJson());
        final User user = userService.getContact(input);

        if (UOpts.Gui.is(user)) {
            final UpdateRef finalInput = input;
            CompletableFuture.runAsync(() -> guiService.handle(finalInput, user));
        } else if (input.getMessage() != null) {
            final MessageRef message = input.getMessage();
            final TeleFile teleFile = message.getTeleFile();
            final String cmd = notNull(input.getMessage().getText()).replaceAll(Pattern.quote("\\\""), "\"");
            logger.debug("Raw cmd: " + cmd);

            if (teleFile != null)
                CompletableFuture.runAsync(() -> {
                    try {
                        fsService.upload(new TFile(teleFile), user);
                    } catch (final Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            else if (!cmd.isEmpty() && cmd.length() < 256)
                CompletableFuture.runAsync(() -> {
                    try {
                        cmdService.handleCmd(cmd.replaceAll("\\s+", " ").trim(), user);
                    } catch (final Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
        }

        return ok();
    }
}
