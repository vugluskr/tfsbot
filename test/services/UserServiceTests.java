package services;

import model.User;
import model.telegram.api.UpdateRef;
import org.junit.Test;
import play.libs.Json;
import util.ATest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class UserServiceTests extends ATest {
    @Test
    public void testInit() {
        preventClean.compareAndSet(true, false);
        doRollback();

        final User user = mkUser();

        userService.getContact(Json.fromJson(Json.parse("{\"update_id\":125898170,\"message\":{\"message_id\":44,\"from\":{\"id\":7777777,\"is_bot\":false,\"first_name\":\"Denis\",\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"language_code\":\"ru\"},\"chat\":{\"id\":7777777,\"first_name\":\"Denis\",\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"type\":\"private\"},\"date\":1588605341,\"photo\":[{\"file_id\":\"AgACAgIAAxkBAAMsXrAxnMjVRHJEKl6jA93nS3AbRxgAAnOtMRsjRoFJXItH1OVo4yVuJ8EOAAQBAAMCAANtAAPz0wUAARkE\",\"file_unique_id\":\"AQADbifBDgAE89MFAAE\",\"file_size\":7583,\"width\":118,\"height\":320},{\"file_id\":\"AgACAgIAAxkBAAMsXrAxnMjVRHJEKl6jA93nS3AbRxgAAnOtMRsjRoFJXItH1OVo4yVuJ8EOAAQBAAMCAAN4AAP10wUAARkE\",\"file_unique_id\":\"AQADbifBDgAE9dMFAAE\",\"file_size\":37768,\"width\":294,\"height\":800},{\"file_id\":\"AgACAgIAAxkBAAMsXrAxnMjVRHJEKl6jA93nS3AbRxgAAnOtMRsjRoFJXItH1OVo4yVuJ8EOAAQBAAMCAAN5AAP00wUAARkE\",\"file_unique_id\":\"AQADbifBDgAE9NMFAAE\",\"file_size\":65690,\"width\":471,\"height\":1280}]}}"), UpdateRef.class));
        final User db = userMapper.selectUser(user.getId());
        assertNotNull(db);
        assertEquals(db.getId(), user.getId());
        assertEquals(db.getDirId(), user.getDirId());
        assertEquals(db.getNick(), user.getNick());
        assertEquals(db.getPwd(), user.getPwd());
    }

    @Test
    public void testPwd() {
        final User user = mkUser();

        userMapper.insertUser(user);

        user.setPwd("/tmp/gogo");
        userService.updatePwd(user);

        final User db = userMapper.selectUser(user.getId());
        assertNotNull(db);
        assertEquals(db.getPwd(), user.getPwd());
    }

}
