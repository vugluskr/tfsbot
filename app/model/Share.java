package model;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.05.2020
 * tfs ☭ sweat and blood
 */
public class Share implements Comparable<Share> {
    private String id;

    private String name, fromName;
    private UUID entryId;
    private long owner, sharedTo;

    private boolean readWrite;

    public String getFromName() { // todo
        return fromName;
    }

    public void setFromName(final String fromName) {
        this.fromName = fromName;
    }

    public boolean isReadWrite() {
        return readWrite;
    }

    public void setReadWrite(final boolean readWrite) {
        this.readWrite = readWrite;
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(final long owner) {
        this.owner = owner;
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

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(final UUID entryId) {
        this.entryId = entryId;
    }

    public boolean isPersonal() {
        return sharedTo > 0;
    }

    public long getSharedTo() {
        return sharedTo;
    }

    public void setSharedTo(final long sharedTo) {
        this.sharedTo = sharedTo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Share share = (Share) o;
        return owner == share.owner && sharedTo == share.sharedTo && id.equals(share.id) && entryId.equals(share.entryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entryId, owner, sharedTo);
    }

    @Override
    public int compareTo(final Share o) {
        if (sharedTo != o.sharedTo)
            return Long.compare(sharedTo, o.sharedTo);

        return Integer.compare(hashCode(), o.hashCode());
    }
}
