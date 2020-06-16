package model;

import services.TgApi;
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
    changeRo(null),
    mkDir(Strings.Uni.folder),
    mkLabel(Strings.Uni.label),
    gear(Strings.Uni.gear),
    Void(Strings.Uni.cancel),
    cancelShare(Strings.Uni.cancel),
    dropShare(Strings.Uni.drop),
    mkGrant(Strings.Uni.mkGrant),
    dropGlobLink(Strings.Uni.link),
    makeGlobLink(Strings.Uni.link),

    share(Strings.Uni.share),
    dropDir(Strings.Uni.drop),
    dropFile(Strings.Uni.drop),
    dropLabel(Strings.Uni.drop),
    renameDir(Strings.Uni.edit),
    renameFile(Strings.Uni.edit),
    editLabel(Strings.Uni.edit),
    openDir(null),
    openFile(null),
    openLabel(null),
    openSearchedDir(null),
    openSearchedFile(null),
    openSearchedLabel(null),
    backToSearch(Strings.Uni.goUp),
    resetToRoot(null),
    joinPublicShare(null),
    doSearch(null),
    uploadFile(null), cancelSearch(Strings.Uni.goUp), contextHelp(null), grantAccess(null), unlock(Strings.Uni.lock), lock(Strings.Uni.keyLock), unlockFile(null), unlockDir(null);

    private final String icon;
    private TgApi.Button button;

    CommandType(final String icon) {this.icon = icon;}

    public TgApi.Button b() {
        if (icon == null)
            return null;

        if (button == null)
            button = new TgApi.Button(icon, this);

        return button;
    }

    public TgApi.Button b(final int idx) {
        return icon == null ? null : new TgApi.Button(icon, toString() + idx);
    }

    public TgApi.Button b(final String label, final int idx) {
        return icon != null ? null : new TgApi.Button(label, toString() + idx);
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
