package model;


import utils.TextUtils;

import java.util.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class User {
    private long id;
    private String nick;
    private int options;
    private int mode;
    private long lastMessageId;
    private long lastDialogId;

    private String pwd;
    private long dirId;
    public final SortedSet<UserAlias> aliases;

    {
        aliases = new TreeSet<>();
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(final String nick) {
        this.nick = nick;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(final int options) {
        this.options = options;
    }

    public String getPwd() {
        return isAtHome() ? "/" : pwd;
    }

    public void setPwd(final String pwd) {
        this.pwd = pwd;
    }

    public long getDirId() {
        return dirId;
    }

    public void setDirId(final long dirId) {
        this.dirId = dirId;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(final int mode) {
        this.mode = mode;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(final long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public long getLastDialogId() {
        return lastDialogId;
    }

    public void setLastDialogId(final long lastDialogId) {
        this.lastDialogId = lastDialogId;
    }

    public String prompt() {
        return "*" + TextUtils.escapeMd(nick) + "@tfs:__" + (isAtHome() ? "\\~" : TextUtils.escapeMd(pwd)) + "__$*";
    }

    public boolean isAtHome() {
        return dirId == 1;
    }
}
