package model.telegram.commands;

import model.User;
import utils.UOpts;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchReq extends ACommand implements TgCommand, RequestUserInput, OfCallback {
    public SearchReq(final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        UOpts.WaitSearchQuery.set(this);
    }

    public static String mnemonic() {
        return SearchReq.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }
    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
