package util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import model.User;
import model.telegram.api.ChatRef;
import model.telegram.api.ContactRef;
import modules.BatisModule;
import org.junit.After;
import org.junit.Before;
import play.Application;
import play.Environment;
import play.inject.Bindings;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.test.WithApplication;
import services.CmdService;
import services.FsService;
import services.TgApi;
import services.UserService;
import services.impl.TgApiFake;
import services.impl.TgApiReal;
import sql.FsMapper;
import sql.TestMapper;
import sql.UserMapper;

import java.util.concurrent.atomic.AtomicBoolean;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class ATest extends WithApplication {
    protected final ContactRef contact = Json.fromJson(Json.parse("{\"id\":7777777,\"is_bot\":false,\"first_name\":\"Denis\",\"last_name\":\"Danilin\"," +
            "\"username\":\"piu_piu_laser\",\"language_code\":\"ru\"}"), ContactRef.class);
    protected final ChatRef chat = Json.fromJson(Json.parse("{\"id\":7777777,\"first_name\":\"Denis\",\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"type\":\"private\"}"), ChatRef.class);

    protected final AtomicBoolean preventClean = new AtomicBoolean(false), initStruct = new AtomicBoolean(false);

    @Inject
    protected TestMapper testMapper;

    @Inject protected FsMapper fsMapper;
    @Inject protected UserMapper userMapper;

    @Inject protected CmdService cmdService;
    @Inject protected FsService fsService;
    @Inject protected UserService userService;

    @Override
    protected Application provideApplication() {
        final AbstractModule batis = new BatisModule();

        final GuiceApplicationBuilder builder = new GuiceApplicationBuilder()
                .in(Environment.simple())
                .overrides(batis)
                .overrides(Bindings.bind(TgApi.class).to(TgApiFake.class));


        Guice.createInjector(builder.applicationModule()).injectMembers(this);

        return builder.build();
    }

    @After
    public void doRollback() {
        if (preventClean.get())
            return;

        try { testMapper.dropFsTree(contact.getId()); } catch (final Exception ignore) { }
        try { testMapper.dropFsStruct(contact.getId()); } catch (final Exception ignore) { }
        testMapper.dropUser(contact.getId());
    }

    @Before
    public void doPrepare() {
        if (!initStruct.get()) return;

        try { testMapper.dropFsTree(contact.getId()); } catch (final Exception ignore) { }
        try { testMapper.dropFsStruct(contact.getId()); } catch (final Exception ignore) { }
        fsService.init(contact.getId());
    }

    protected User mkUser() {
        final User user = new User();
        user.setId(contact.getId());
        user.setNick(notNull(contact.getUsername(), "u" + contact.getId()));
        user.setDirId(1);
        user.setPwd("/");

        return user;
    }
}
