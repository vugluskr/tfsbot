package model.telegram.commands;

import model.User;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class ACommand extends User implements TgCommand {
    protected final long msgId, callbackId;

    protected ACommand(final long msgId, final User user, final long callbackId) {
        this.msgId = msgId;
        this.callbackId = callbackId;

        setId(user.getId());
        setDirId(user.getDirId());
        setLastDialogId(user.getLastDialogId());
        setLastMessageId(user.getLastMessageId());
        setOptions(user.getOptions());
        setOffset(user.getOffset());
        setLastSearch(user.getLastSearch());
        setMode(user.getMode());
        setPwd(user.getPwd());
        setNick(user.getNick());
    }

    @Override
    public final long getMsgId() {
        return msgId;
    }

    @Override
    public final long getCallbackId() {
        return callbackId;
    }
}
