package modules;

import com.google.inject.AbstractModule;
import services.Converter1to2;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.06.2020
 * tfs ☭ sweat and blood
 */
public class ConverterModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Converter1to2.class).asEagerSingleton();
    }
}
