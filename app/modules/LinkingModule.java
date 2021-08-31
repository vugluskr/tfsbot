package modules;

import com.google.inject.AbstractModule;

public class LinkingModule extends AbstractModule {
    @Override
    protected void configure() {
            bind(Jobs.class).asEagerSingleton();
    }
}
