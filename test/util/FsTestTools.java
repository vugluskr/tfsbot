package util;

import model.TFile;
import model.telegram.ContentType;

import static org.junit.Assert.assertNotNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class FsTestTools extends ATest {
    protected TFile docs, usr, scans, blackwhite, colour, file;

    protected void initFs(final boolean withFile) {
        docs = mkdirDb("docs", 1);
        assertNotNull(docs);

        usr = mkdirDb("usr", 1);
        assertNotNull(usr);

        scans = mkdirDb("scans", docs.getId());
        assertNotNull(scans);

        blackwhite = mkdirDb("blackwhite", scans.getId());
        assertNotNull(blackwhite);

        colour = mkdirDb("colour", scans.getId());
        assertNotNull(colour);

        if (withFile) {
            file = mkfileDb("aidio.mp3", scans.getId());
            assertNotNull(file);
        }
    }

    protected TFile mkdirDb(final String name, final long parentId) {
        final TFile tmp = mkdir(name, parentId);
        fsMapper.mkDir(tmp.getName(), tmp.getParentId(), tmp.getIndate(), tmp.getType().name(), contact.getId());

        return fsMapper.findEntryAt(tmp.getName(), tmp.getParentId(), contact.getId());
    }

    protected TFile mkdir(final String name, final long parentId) {
        final TFile dir = new TFile();
        dir.setType(ContentType.DIR);
        dir.setIndate(System.currentTimeMillis());
        dir.setName(name);
        dir.setParentId(parentId);

        return dir;
    }

    protected TFile mkfileDb(final String name, final long parentId) {
        final TFile file = mkfile(name, parentId);
        fsMapper.mkFile(file, contact.getId());

        return fsMapper.findEntryAt(file.getName(), file.getParentId(), contact.getId());
    }

    protected TFile mkfile(final String name, final long parentId) {
        final TFile file = new TFile();
        file.setType(ContentType.AUDIO);
        file.setIndate(System.currentTimeMillis());
        file.setName(name);
        file.setParentId(parentId);
        file.setRefId("saoeifu98327uhsdf");
        file.setSize(8888888L);

        return file;
    }

}
