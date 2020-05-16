package model.telegram.commands;

import model.User;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public final class MoveEntry extends ACommand implements TgCommand, SingleEntryOp, OfCallback {
    public final long entryId;

    public MoveEntry(final long entryId, final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        this.entryId = entryId;
    }

    @Override
    public long getEntryId() {
        return entryId;
    }

    public static String mnemonic() {
        return MoveEntry.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }
    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
