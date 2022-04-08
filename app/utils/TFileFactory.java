package utils;

import model.ContentType;
import model.TFile;

import java.util.UUID;

import static utils.TextUtils.generateUuid;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.05.2020
 * tfs ☭ sweat and blood
 */
public class TFileFactory {
    public static TFile label(final String name, final UUID parentId, final long owner) {
        final TFile f = new TFile();
        f.setOwner(owner);
        f.setRw(true);
        f.setType(ContentType.LABEL);
        f.setParentId(parentId);
        f.setName(name);
        f.setRefId("--");
        f.setId(generateUuid());

        return f;
    }

    public static TFile genresDir(final String name, final UUID parentId, final long owner) {
        final TFile dir = dir(name, parentId, owner);
        dir.setGenres();
        return dir;
    }

    public static TFile authorsDir(final String name, final UUID parentId, final long owner) {
        final TFile dir = dir(name, parentId, owner);
        dir.setAuthors();
        return dir;
    }

    public static TFile abcDir(final String name, final UUID parentId, final long owner) {
        final TFile dir = dir(name, parentId, owner);
        dir.setAbc();
        return dir;
    }

    public static TFile dir(final String name, final UUID parentId, final long owner) {
        final TFile f = new TFile();
        f.setOwner(owner);
        f.setRw(true);
        f.setType(ContentType.DIR);
        f.setParentId(parentId);
        f.setName(name);
        f.setRefId("--");
        f.setId(generateUuid());

        return f;
    }

    public static TFile file(final String title, final UUID parentDirId, final long owner, final String refId) {
        final TFile file = new TFile();
        file.setOwner(owner);
        file.setName(title);
        file.setParentId(parentDirId);
        file.setType(ContentType.DOCUMENT);
        file.setRefId(refId);

        return file;
    }

    public static TFile softLink(final TFile trg, final UUID parent) {
        final TFile link = new TFile();
        link.setOwner(trg.getOwner());
        link.setName(trg.getName());
        link.setParentId(parent);
        link.setType(ContentType.SOFTLINK);
        link.setRefId(trg.getId().toString());

        return link;
    }

    public static TFile softLink(final String name, final String refId, final UUID parentDirId, final long ownerId) {
        final TFile link = new TFile();
        link.setOwner(ownerId);
        link.setName(name);
        link.setParentId(parentDirId);
        link.setType(ContentType.SOFTLINK);
        link.setRefId(refId);

        return link;
    }
}
