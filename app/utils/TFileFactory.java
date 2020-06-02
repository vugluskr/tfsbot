package utils;

import model.TFile;
import model.telegram.ContentType;
import model.telegram.api.TeleFile;

import java.util.UUID;

import static utils.TextUtils.generateUuid;
import static utils.TextUtils.notNull;

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

    public static TFile file(final TeleFile src, final String caption, final UUID parentId, final long owner) {
        final ContentType type = src.getType();

        final TFile f = new TFile();
        f.setOwner(owner);
        f.setRw(true);
        f.setType(src.getType());
        f.setParentId(parentId);
        f.setName(notNull(notNull(src.getFileName(), caption), type.name().toLowerCase() + "_" + src.getUniqId() + type.ext));
        f.setRefId(src.getFileId());
        f.setId(generateUuid());

        return f;

    }
}
