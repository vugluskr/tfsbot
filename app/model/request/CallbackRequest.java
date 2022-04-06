package model.request;

import com.fasterxml.jackson.databind.JsonNode;
import model.Command;
import model.CommandType;
import play.Logger;

import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 15:09
 * tfs ☭ sweat and blood
 */
public class CallbackRequest extends TgRequest {
    private static final Logger.ALogger logger = Logger.of(CallbackRequest.class);
    public final long queryId;

    private Command command;

    public CallbackRequest(final JsonNode node) {
        super(node.get("from"));
        queryId = node.get("id").asLong();
        command = null;

        final String cb = node.get("data").asText();

        final int del = cb.indexOf(':');

        if (del > 0) {
            command = new Command();
            command.elementIdx = del < cb.length() - 1 ? getInt(cb.substring(del + 1)) : -1;
            command.type = CommandType.ofString(cb);
        } else
            logger.debug("Cant resolve '" + node + "' to callback. From user " + user);
    }

    @Override
    public boolean isCrooked() {
        return command == null;
    }

    public Command getCommand() {
        return command;
    }
}
