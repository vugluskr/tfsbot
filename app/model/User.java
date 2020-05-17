package model;


import utils.State;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class User {
    private long id;
    private String nick;
    private int options;
    private long lastMessageId;
    private long lastDialogId;
    private String savedState;

    private State state;

    public long getId() {
        return id;
    }

    public String getSavedState() {
        return savedState;
    }

    public void setSavedState(final String savedState) {
        this.savedState = savedState;
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

//    public String getPwd() {
//        return isAtHome() ? "/" : pwd;
//    }
//
//    public void setPwd(final String pwd) {
//        this.pwd = pwd;
//    }
//
//    public long getDirId() {
//        return dirId;
//    }
//
//    public void setDirId(final long dirId) {
//        this.dirId = dirId;
//    }
//
//    public int getMode() {
//        return mode;
//    }
//
//    public void setMode(final int mode) {
//        this.mode = mode;
//    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(final long lastMessageId) {
//        if (lastMessageId != this.lastMessageId)
//            setOffset(0);
        this.lastMessageId = lastMessageId;
    }

    public long getLastDialogId() {
        return lastDialogId;
    }

    public void setLastDialogId(final long lastDialogId) {
        this.lastDialogId = lastDialogId;
    }

//    public boolean isAtHome() {
//        return dirId == 1;
//    }

//    public int getOffset() {
//        return offset;
//    }
//
//    public void setOffset(final int offset) {
//        this.offset = offset;
//    }
//
//    public void setMode(final UserMode mode) {
//        this.mode = mode.ordinal();
//    }
//
//    public UserMode getUserMode() {
//        return UserMode.values()[Math.max(0, Math.min(UserMode.values().length - 1, mode))];
//    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }
}
