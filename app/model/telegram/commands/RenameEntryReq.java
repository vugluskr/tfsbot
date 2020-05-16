package model.telegram.commands;

import model.User;
import utils.UOpts;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class RenameEntryReq extends ACommand implements TgCommand, RequestUserInput, OfCallback {
    public final long entryId;

    public RenameEntryReq(final long entryId, final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        this.entryId = entryId;
        UOpts.WaitFileName.set(this);
    }

    public static String mnemonic() {
        return RenameEntryReq.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }
    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
