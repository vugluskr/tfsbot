package services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import model.TFile;
import model.telegram.api.TextRef;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import services.TgApi;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class TgApiReal implements TgApi {
    private static final Logger.ALogger logger = Logger.of(TgApiReal.class);

    private final String apiUrl;
    private final WSClient ws;

    @Inject
    public TgApiReal(final Config config, final WSClient ws) {
        this.ws = ws;
        apiUrl = config.getString("service.bot.api_url");
    }

    @Override
    public void sendFile(final TFile file, final long chatId) {
        if (file == null)
            return;

        final ObjectNode node = Json.newObject();
        node.put("chat_id", chatId);
        node.put(file.getType().getParamName(), file.getRefId());

        ws.url(apiUrl + file.getType().getUrlPath())
                .post(node)
                .thenAccept(wsr -> logger.debug("API call: send" + file.getType().getUrlPath() + "\n" + node + "\nResponse: " + wsr.getBody()));
    }

    @Override
    public void sendMessage(final TextRef text) {
        if (isEmpty(text) || isEmpty(text.getText()))
            return;

        final JsonNode node = Json.toJson(text);

        ws.url(apiUrl + "sendMessage")
                .post(node)
                .thenAccept(wsr -> logger.debug("API call: sendMessage\n" + node + "\nResponse: " + wsr.getBody()));
    }
}
