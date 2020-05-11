package model.telegram.api;

import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public interface TeleFile {
    ContentType getType();
    long getFileSize();
    String getFileId();
    String getUniqId();
    default String getFileName() {
        return null;
    }
}
