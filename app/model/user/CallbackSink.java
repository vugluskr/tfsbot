package model.user;

import model.Command;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.06.2020
 * tfs ☭ sweat and blood
 */
public interface CallbackSink {
    void onCallback(Command command);
}
