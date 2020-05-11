package mappers;

import model.TFile;
import org.junit.Test;
import util.FsTestTools;

import static org.junit.Assert.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class FsMapperTests extends FsTestTools {
    public FsMapperTests() {
        initStruct.set(false);
    }

    @Test
    public void structTest() {
        doRollback();

        assertFalse(fsMapper.isFsTableExist(contact.getId()));
        fsMapper.createUserFs(contact.getId());
        assertTrue(fsMapper.isFsTableExist(contact.getId()));

        fsMapper.createRoot(contact.getId());
        fsMapper.createTree(contact.getId());
    }

    @Test
    public void mkdirTest() {
        final TFile dir = mkdir("test", 1);

        if (!fsMapper.isFsTableExist(contact.getId())) {
            initStruct.set(true);
            doPrepare();
            initStruct.set(false);
        }

        fsMapper.mkDir(dir.getName(), dir.getParentId(), dir.getIndate(), dir.getType().name(), contact.getId());

        assertNotNull(fsMapper.findEntryByPath("/" + dir.getName(), contact.getId()));
    }

    @Test
    public void mkFileTest() {
        final TFile file = mkfile("audio_666666", 1);

        if (!fsMapper.isFsTableExist(contact.getId())) {
            initStruct.set(true);
            doPrepare();
            initStruct.set(false);
        }

        fsMapper.mkFile(file, contact.getId());

        assertNotNull(fsMapper.findEntryByPath("/" + file.getName(), contact.getId()));
    }

    @Test
    public void browseTest() {
        preventClean.compareAndSet(true, false);
        doRollback();
        if (!fsMapper.isFsTableExist(contact.getId())) {
            initStruct.set(true);
            doPrepare();
            initStruct.set(false);
        }

        initFs(true);

        assertEquals(docs, fsMapper.getEntry(docs.getId(), contact.getId()));
        assertEquals(blackwhite, fsMapper.findEntryByPath(blackwhite.getPath(), contact.getId()));
        assertEquals(colour, fsMapper.findEntryAt("colour", scans.getId(), contact.getId()));

        fsMapper.listEntries(scans.getId(), contact.getId()).forEach(d -> assertTrue(d.equals(colour) || d.equals(blackwhite) || d.equals(file)));
    }

    @Test
    public void crudTest() {
        preventClean.compareAndSet(true, false);
        doRollback();
        if (!fsMapper.isFsTableExist(contact.getId())) {
            initStruct.set(true);
            doPrepare();
            initStruct.set(false);
        }

        initFs(true);

        file.setName("old_autio.mp3");
        file.setParentId(colour.getId());

        fsMapper.updateEntry(file.getName(), System.currentTimeMillis() - 30000, file.getParentId(), file.getId(), contact.getId());

        final TFile modif = fsMapper.findEntryAt(file.getName(), colour.getId(), contact.getId());
        assertNotNull(modif);
        assertEquals(file, modif);
        assertNotEquals(file.getIndate(), modif.getIndate());

        fsMapper.dropEntry(blackwhite.getId(), contact.getId());
        assertNull(fsMapper.findEntryAt(blackwhite.getName(), blackwhite.getParentId(), contact.getId()));

        fsMapper.dropOrphans(scans.getId(), contact.getId());
        assertNull(fsMapper.findEntryAt(file.getName(), colour.getId(), contact.getId()));
        assertNull(fsMapper.findEntryAt(colour.getName(), scans.getId(), contact.getId()));
    }
}
