package model.telegram.api;

/**
 * @author Denis Danilin | denis@danilin.name
 * 13.05.2020
 * tfs ☭ sweat and blood
 */
public interface Screen {
    String message();
    ReplyMarkup markup();
    String parseMode();
}
