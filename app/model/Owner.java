package model;

import utils.UserMode;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public interface Owner {
    long getId();

    void setLastDialogId(long dialogId);

    long getLastDialogId();

    long getDirId();

    long getLastMessageId();

    void setLastMessageId(long msgId);

    void setMode(UserMode mode);

    void setOffset(int offset);

    int getOptions();

    int getMode();

    int getOffset();

    void setLastSearch(String query);

    String getLastSearch();
}
