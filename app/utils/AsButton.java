package utils;

import services.BotApi;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 16:49
 * tfs ☭ sweat and blood
 */
public interface AsButton {
    BotApi.Button toButton(int idx);
}
