package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public enum Cmd { // todo a lot
    get(true, 1, 1),
    ls("laN", false, 0, Integer.MAX_VALUE),
    mv(true, 2, Integer.MAX_VALUE),
    rm("rf", 1, Integer.MAX_VALUE),
//    ln,
    mkdir("p", 1, 1),
//    chmod(true, 2, 2),
    cd(false, 1, 1),
    pwd,
    alias(true, 2, 2),
//    pushd("n+-"),
//    touch(true, 1, Integer.MAX_VALUE),
//    date,
//    du("sh"),
//    usermod("l", 1, 1),
//    tree(""),
    help(false, 0, 1);

    private static final Set<String> names = Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());

    private final char[] opts;
    private final boolean requireArgs;
    private final int minArgs, maxArgs;

    Cmd() {
        opts = new char[0];
        requireArgs = false;
        minArgs = maxArgs = 0;
    }

    Cmd(final String opts) {
        this.opts = opts.toCharArray();
        requireArgs = false;
        minArgs = maxArgs = 0;
    }

    Cmd(final String opts, final int minArgs, final int maxArgs) {
        this.opts = opts.toCharArray();
        this.requireArgs = minArgs > 0 || maxArgs > 0;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    Cmd(final String opts, final boolean requireArgs, final int minArgs, final int maxArgs) {
        this.opts = opts.toCharArray();
        this.requireArgs = requireArgs;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    Cmd(final boolean requireArgs, final int minArgs, final int maxArgs) {
        this.opts = new char[0];
        this.requireArgs = requireArgs;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }
}
