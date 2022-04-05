package model;

import services.BotApi;
import utils.Strings;

import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | denis@danilin.name
 * 11.06.2020
 * tfs ☭ sweat and blood
 */
public enum CommandType {
    openParent(Strings.Uni.goUp),
    rewind(Strings.Uni.rewind),
    forward(Strings.Uni.forward),
    changeGrantRw(null),
    mkDir(Strings.Uni.folder),
    mkLabel(Strings.Uni.label),
    gearDir(Strings.Uni.gear),
    Void(Strings.Uni.cancel),
    cancel(Strings.Uni.cancel),
    dropGrant(null),
    mkGrant(Strings.Uni.mkGrant),
    dropEntryLink(Strings.Uni.link),
    makeEntryLink(Strings.Uni.link),

    share(Strings.Uni.share),
    drop(Strings.Uni.drop),
    dropFile(Strings.Uni.drop),
    dropLabel(Strings.Uni.drop),
    rename(Strings.Uni.edit),
    renameFile(Strings.Uni.edit),
    editLabel(Strings.Uni.edit),
    openDir(null),
    openFile(null),
    openLabel(null),
    openSearchedDir(null),
    openSearchedFile(null),
    openSearchedLabel(null),
    backToSearch(Strings.Uni.goUp),
    joinPublicShare(null),
    doSearch(null),
    uploadFile(null),
    exitSearch(Strings.Uni.goUp),
    contextHelp(Strings.Uni.put),
    grantAccess(null),
    unlock(Strings.Uni.lock),
    lock(Strings.Uni.keyLock),
    unlockFile(null),
    unlockDir(null),
    reOpds(Strings.Uni.syncOpds),
    openShare(null),
    setBooks(Strings.Uni.bookStore),
    rewindGearDir(Strings.Uni.rewind),
    forwardGearDir(Strings.Uni.forward),
    rewindShares(Strings.Uni.rewind),
    forwardShares(Strings.Uni.forward),
    exitLabelGear(Strings.Uni.goUp),
    exitGearShares(Strings.Uni.goUp),
    goBack(Strings.Uni.goUp),
    confirm(Strings.Uni.put);

    private final String icon;
    private BotApi.Button button;

    CommandType(final String icon) {this.icon = icon;}

    public BotApi.Button b() {
        if (icon == null)
            return null;

        if (button == null)
            button = new BotApi.Button(icon, this);

        return button;
    }

    public BotApi.Button b(final int idx) {
        return icon == null ? null : new BotApi.Button(icon, toString() + idx);
    }

    public BotApi.Button b(final String label, final int idx) {
        return icon != null ? null : new BotApi.Button(label, toString() + idx);
    }

    public static CommandType ofString(final String callback) {
        if (callback.contains(":"))
            return values()[getInt(callback.substring(0, callback.indexOf(':')))];

        return null;
    }

    public String toString() {
        return ordinal() + ":";
    }
}
