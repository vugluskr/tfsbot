package utils;

import model.TFile;
import model.telegram.ContentType;
import model.telegram.api.TeleFile;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.05.2020
 * tfs ☭ sweat and blood
 */
public class TFileFactory {
    public static TFile label(final String name, final long parentId) {
        final TFile f = new TFile();
        f.setType(ContentType.LABEL);
        f.setParentId(parentId);
        f.setName(name);
        f.setRefId("--");

        return f;
    }

    public static TFile dir(final String name, final long parentId) {
        final TFile f = new TFile();
        f.setType(ContentType.DIR);
        f.setParentId(parentId);
        f.setName(name);
        f.setRefId("--");

        return f;
    }

    public static TFile file(final TeleFile src, final long parentId) {
        final ContentType type = src.getType();

        final TFile f = new TFile();
        f.setType(src.getType());
        f.setParentId(parentId);
        f.setName(notNull(src.getFileName(), type.name().toLowerCase() + "_" + src.getUniqId() + type.ext));
        f.setRefId(src.getFileId());

        return f;

    }
}
