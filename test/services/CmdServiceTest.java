package services;

import model.User;
import org.junit.Test;
import services.impl.TgApiFake;
import util.FsTestTools;

import javax.inject.Inject;

import static org.junit.Assert.*;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class CmdServiceTest extends FsTestTools {
    @Inject
    private TgApi tgApi;

    private User user;

    public CmdServiceTest() {
        initStruct.set(true);
    }

    @Test
    public void testget() {
        ((TgApiFake) tgApi).fileSendListener = (tFile, aLong) -> assertEquals(tFile.getId(), file.getId());
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(textRef.getText().contains(escapeMd(user.getNick() + "@"))); // проверка на pwd

        cmdService.handleCmd("get " + file.getPath(), user);
        cmdService.handleCmd("get " + scans.getPath(), user); // должен быть переход в дир.
        assertTrue(user.getPwd().contains(scans.getName()));

        cmdService.handleCmd("get", user); // ничо не должно быть
    }

    @Test
    public void testcd() {
        cmdService.handleCmd("cd", user);
        assertFalse(user.getPwd().contains(scans.getName()));

        cmdService.handleCmd("cd " + scans.getPath(), user);
        assertTrue(user.getPwd().contains(scans.getName()));

        cmdService.handleCmd("cd " + usr.getName(), user); // не должно, /docs/scans и /usr не видны друг другу
        assertTrue(user.getPwd().contains(scans.getName()));

        cmdService.handleCmd("cd " + usr.getPath(), user);
        assertFalse(user.getPwd().contains(scans.getName()));
        assertTrue(user.getPwd().contains(usr.getName()));

        ((TgApiFake) tgApi).fileSendListener = (tFile, aLong) -> assertEquals(tFile.getId(), file.getId());
        cmdService.handleCmd("cd                                " + file.getPath(), user); // должен отдаться файлик
        cmdService.handleCmd(file.getPath(), user); // тоже должен отдаться файлик

        assertTrue(user.getPwd().contains(usr.getName()));
        cmdService.handleCmd("cd", user);
        assertFalse(user.getPwd().contains(usr.getName()));
        cmdService.handleCmd(docs.getName(), user); // переход
        assertTrue(user.getPwd().contains(docs.getName()));
    }

    @Test
    public void testls() {
        cmdService.handleCmd("cd " + scans.getPath(), user);

//        mkfileDb("iomg.jpg", 1);

        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(
//                        textRef.getText().contains(escapeMd(colour.getName() + "/"))
//                        && textRef.getText().contains(escapeMd(blackwhite.getName() + "/"))
                        /*&& */textRef.getText().contains(escapeMd(file.getName())));

//        cmdService.handleCmd("ls", user);
        cmdService.handleCmd("l *.mp3", user);
    }

    @Test
    public void testmkdir() {
        cmdService.handleCmd("mkdir", user);
        cmdService.handleCmd("mkdir /docs/docs2/docs3", user);
        cmdService.handleCmd("cd docs3", user);
        assertFalse(user.getPwd().contains("docs3"));
        cmdService.handleCmd("cd docs/docs2/docs3", user);
        assertTrue(user.getPwd().contains("docs3"));
    }

    @Test
    public void testmv() {
        ((TgApiFake) tgApi).msgSendListener = textRef -> {throw new RuntimeException("Какого-то хера посылаем чё-то: " + textRef.getText());};
        cmdService.handleCmd("mv", user); // ничего
        cmdService.handleCmd("mv 1", user);  // ничего
        cmdService.handleCmd("mv 1111 docs/", user); // ничего
        ((TgApiFake) tgApi).msgSendListener = null;

        cmdService.handleCmd("mv " + colour.getPath() + " " + usr.getPath(), user);
        cmdService.handleCmd("cd " + usr.getPath(), user);
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(textRef.getText().contains(colour.getName()));
        cmdService.handleCmd("ls", user);
        ((TgApiFake) tgApi).msgSendListener = null;

        cmdService.handleCmd("mv " + file.getPath() + " " + docs.getPath(), user);
        cmdService.handleCmd("cd " + docs.getPath(), user);
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(textRef.getText().contains(escapeMd(file.getName())));
        cmdService.handleCmd("ls", user);
        ((TgApiFake) tgApi).msgSendListener = null;

        cmdService.handleCmd("mv " + file.getName() + "  newnewnew.file", user);
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(textRef.getText().contains(escapeMd("newnewnew.file")));
        cmdService.handleCmd("ls", user);
        ((TgApiFake) tgApi).msgSendListener = null;

        cmdService.handleCmd("mv " + scans.getPath() + "/* " + docs.getPath(), user);
        cmdService.handleCmd("cd " + docs.getPath(), user);
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(
                /*textRef.getText().contains(colour.getName())
                && */textRef.getText().contains(blackwhite.getName()));
        cmdService.handleCmd("ls", user);
    }

    @Test
    public void testpwd() {
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertEquals(textRef.getText(), "`" + user.getPwd() + "`");
        cmdService.handleCmd("pwd", user);
    }

    @Test
    public void testrm() {
        cmdService.handleCmd("rm " + docs.getName(), user);
        ((TgApiFake) tgApi).msgSendListener = textRef -> assertTrue(
                textRef.getText().contains(usr.getName())
                        && !textRef.getText().contains(blackwhite.getName())
                        && !textRef.getText().contains(docs.getName())
                        && !textRef.getText().contains(scans.getName())
                        && !textRef.getText().contains(file.getName())
                        && !textRef.getText().contains(colour.getName())
                                                                   );
        cmdService.handleCmd("ls", user);
    }

    @Override
    public void doPrepare() {
        super.doPrepare();
        user = mkUser();
        userMapper.insertUser(user);
        initFs(true);
    }

}
