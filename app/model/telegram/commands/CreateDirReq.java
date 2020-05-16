package model.telegram.commands;

import model.User;
import utils.UOpts;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class CreateDirReq extends ACommand implements TgCommand, RequestUserInput, OfCallback {
    public CreateDirReq(final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        UOpts.WaitFolderName.set(this);
    }

    public static String mnemonic() {
        return CreateDirReq.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }

    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
