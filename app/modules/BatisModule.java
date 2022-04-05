package modules;

import com.google.inject.name.Names;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import play.db.Database;
import sql.TfsMapper;
import utils.UUIDTypeHandler;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 12.09.2017 14:09
 * core-router ☭ sweat and blood
 */
public class BatisModule extends org.mybatis.guice.MyBatisModule {

    @Override
    protected void initialize() {
        environmentId("development");
        bindConstant().annotatedWith(
                Names.named("mybatis.configuration.failFast")).
                to(true);
        bindDataSourceProviderType(PlayDataSourceProvider.class);
        bindTransactionFactoryType(JdbcTransactionFactory.class);
        addTypeHandlerClass(UUIDTypeHandler.class);
        addSimpleAlias(UUIDTypeHandler.class);
        addSimpleAlias(UUID.class);
        addMapperClasses(TfsMapper.class.getPackage().getName());
    }

    @Singleton
    public static class PlayDataSourceProvider implements Provider<DataSource> {
        final Database db;

        @Inject
        public PlayDataSourceProvider(final Database db) {
            this.db = db;
        }

        @Override
        public DataSource get() {
            return db.getDataSource();
        }
    }

}
