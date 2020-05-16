package model.telegram.commands;

import model.Owner;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public interface TgCommand extends Owner {
    long getMsgId();

    long getCallbackId();
}
