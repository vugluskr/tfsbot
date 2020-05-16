package model.telegram.commands;

import model.User;
import model.telegram.ContentType;
import model.telegram.api.TeleFile;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public final class CreateFile extends ACommand implements TgCommand, TeleFile {
    public final String name;
    public final String type;
    public final String refId;
    public final String uniqId;
    public final long size;

    public CreateFile(final String name, final String type, final String refId, final String uniqId, final long size, final long msgId, final long callbackId, final User user) {
        super(msgId, user, callbackId);
        this.name = name;
        this.type = type;
        this.refId = refId;
        this.uniqId = uniqId;
        this.size = size;
    }

    @Override
    public ContentType getType() {
        return ContentType.valueOf(type);
    }

    @Override
    public long getFileSize() {
        return size;
    }

    @Override
    public String getFileId() {
        return refId;
    }

    @Override
    public String getUniqId() {
        return uniqId;
    }

    @Override
    public String getFileName() {
        return name;
    }
}
