package model;


import utils.TextUtils;

import java.util.SortedSet;
import java.util.TreeSet;

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
    private int offset;
    private long lastMessageId;
    private long lastDialogId;
    private long lastHit;

    private String pwd, selection;
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
        if (lastMessageId != this.lastMessageId)
            setOffset(0);
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

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public long getLastHit() {
        return lastHit;
    }

    public void setLastHit(final long lastHit) {
        this.lastHit = lastHit;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(final String selection) {
        this.selection = selection;
    }

    public long getDelay() {
        try {
            return lastHit <= 0 ? 0 : System.currentTimeMillis() - lastHit;
        } finally {
            lastHit = System.currentTimeMillis() + 1000;
        }
    }
}
