package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.01.2018 14:34
 * SIRBot ☭ sweat and blood
 */
public class InlineKeyboard implements ReplyMarkup {
    @JsonProperty("inline_keyboard")
    private List<List<InlineButton>> keyboard;

    public InlineKeyboard() {
    }

    public InlineKeyboard(final List<List<InlineButton>> keyboard) {
        this.keyboard = keyboard;
    }

    public List<List<InlineButton>> getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(final List<List<InlineButton>> keyboard) {
        this.keyboard = keyboard;
    }
}
