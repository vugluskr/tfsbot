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

    public static TFile opdsDir(final String name, final String url, final UUID parentId, final long owner) {
        final TFile f = dir(name, parentId, owner);
        f.asOpds(url);

        return f;
    }
}
