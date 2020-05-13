package services.impl;

import model.TFile;
import model.telegram.api.*;
import play.Logger;
import play.libs.Json;
import services.TgApi;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
@Singleton
public class TgApiFake implements TgApi {
    private final static Logger.ALogger logger = Logger.of(TgApiFake.class);

    public volatile BiConsumer<TFile, Long> fileSendListener = null;
    public volatile Consumer<TextRef> msgSendListener = null;

    @Override
    public void sendFile(final TFile file, final long chatId) {
        logger.info("Send file to chat '" + chatId + "': " + file);

        if (fileSendListener != null)
            fileSendListener.accept(file, chatId);
    }

    @Override
    public CompletionStage<ApiMessageReply> sendMessage(final TextRef text) {
        if (text == null || isEmpty(text.getText())) {
            logger.warn("Not send empty msg: " + Json.toJson(text));
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Send prewrapped message to chat '" + text.getChatId() + "':\n" + text.getText());
        if (msgSendListener != null)
            msgSendListener.accept(text);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<ApiMessageReply> updateMessage(final UpdateMessage update) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> deleteMessage(final DeleteMessage deleteMessage) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void sendCallbackAnswer(final CallbackAnswer callbackAnswer) {

    }
}
