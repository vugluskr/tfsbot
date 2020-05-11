package mappers;

import model.User;
import org.junit.Test;
import util.ATest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class UserMapperTests extends ATest {

    @Test
    public void testInit() {
        final User user = mkUser();

        userMapper.insertUser(user);
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
        userMapper.updatePwd(user);

        final User db = userMapper.selectUser(user.getId());
        assertNotNull(db);
        assertEquals(db.getPwd(), user.getPwd());
    }
}
