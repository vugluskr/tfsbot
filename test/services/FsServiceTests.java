package services;

import model.TFile;
import model.User;
import org.junit.Test;
import util.FsTestTools;

import static org.junit.Assert.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class FsServiceTests extends FsTestTools {
    private User user;

    public FsServiceTests() {
        initStruct.set(false);
    }

    @Test
    public void testInit() {
        doRollback();

        assertFalse(fsMapper.isFsTableExist(contact.getId()));
        fsService.init(contact.getId());
        assertTrue(fsMapper.isFsTableExist(contact.getId()));
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

        assertEquals(docs, fsService.get(docs.getId(), user));
        assertEquals(blackwhite, fsService.findPath(blackwhite.getPath(), user));

        user.setDirId(scans.getId());
        fsService.list(user).forEach(d -> assertTrue(d.equals(colour) || d.equals(blackwhite) || d.equals(file)));
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
        file.setParentId(docs.getId());

        fsService.updateMeta(file, user);

        TFile modif = fsService.get(file.getId(), user);
        assertNotNull(modif);
        assertEquals(file, modif);

        fsService.rm(scans.getId(), user);
        assertNull(fsMapper.findEntryAt(blackwhite.getName(), blackwhite.getParentId(), contact.getId()));
        assertNull(fsMapper.findEntryAt(file.getName(), colour.getId(), contact.getId()));
        assertNull(fsMapper.findEntryAt(colour.getName(), scans.getId(), contact.getId()));

        fsService.rm(file.getId(), user);
        assertNull(fsService.findPath(file.getPath(), user));
    }

    @Test
    public void uploadMkdirTest() {
        preventClean.compareAndSet(true, false);
        doRollback();
        if (!fsMapper.isFsTableExist(contact.getId())) {
            initStruct.set(true);
            doPrepare();
            initStruct.set(false);
        }

        initFs(false);

        user.setDirId(docs.getId());
        file = mkfile("aidio.mp3", docs.getId());
        fsService.upload(file, user);

        TFile db = fsService.findHere(file.getName(), user);
        assertNotNull(db);
        assertEquals(file, db);

        final TFile dir = fsService.mkdir("lambada", docs.getId(), user.getId());
        assertNotNull(dir);
        assertTrue(dir.getId() > 0);
        assertEquals(dir.getName(), "lambada");
    }

    @Override
    public void doPrepare() {
        super.doPrepare();
        user = mkUser();

        userMapper.insertUser(user);
    }
}
