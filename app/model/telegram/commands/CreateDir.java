package model.telegram.commands;

import model.User;
import utils.UOpts;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public final class CreateDir extends ACommand implements TgCommand, GotUserInput {
    public final String name;

    public CreateDir(final String name, final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        this.name = name;
        UOpts.WaitFolderName.clear(this);
    }

    @Override
    public String getInput() {
        return name;
    }

    public static String mnemonic() {
        return CreateDir.class.getSimpleName() + '.';
    }

    public static String mnemonic(final long id) {
        return mnemonic() + id;
    }

    public static boolean is(final String data) {
        return notNull(data).startsWith(mnemonic());
    }
}
