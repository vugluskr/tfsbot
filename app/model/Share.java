package model;

import utils.BMasked;
import utils.Optioned;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.05.2020
 * tfs ☭ sweat and blood
 */
public class Share implements Optioned {
    private String id;

    private String name;
    private long entryId, owner, sharedTo;

    private int options;
    private long untill;

    private String salt, hash;

    public boolean isPersonal() {
        return sharedTo > 0;
    }

    public long getSharedTo() {
        return sharedTo;
    }

    public void setSharedTo(final long sharedTo) {
        this.sharedTo = sharedTo;
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(final long owner) {
        this.owner = owner;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(final int options) {
        this.options = options;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getEntryId() {
        return entryId;
    }

    public void setEntryId(final long entryId) {
        this.entryId = entryId;
    }

    public long getUntill() {
        return untill;
    }

    public void setUntill(final long untill) {
        this.untill = untill;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(final String hash) {
        this.hash = hash;
    }

    public boolean isGlobal() {
        return sharedTo == 0;
    }

    public boolean isPasswordLock() {
        return Optz.PassLock.is(this);
    }

    public boolean isOneTime() {
        return Optz.OneTime.is(this);
    }

    public void clearPasswordLock() {
        Optz.PassLock.remove(this);
        salt = null;
        hash = null;
    }

    public void clearValids() {
        Optz.OneTime.remove(this);
        untill = 0;
    }

    public boolean isEdited() {
        return Optz.Edited.is(this);
    }

    public void clearEdited() {
        Optz.Edited.remove(this);
    }

    public void setEdited() {
        Optz.Edited.set(this);
    }

    public void setPasswordLock() {
        Optz.PassLock.set(this);
    }

    enum Optz implements BMasked {
        PassLock, OneTime, Edited;
    }
}
