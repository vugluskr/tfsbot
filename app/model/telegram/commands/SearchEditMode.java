package model.telegram.commands;

import model.User;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchEditMode extends ACommand implements TgCommand, OfCallback {
    public SearchEditMode(final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
    }

    public static String mnemonic() {
        return SearchEditMode.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }

    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
