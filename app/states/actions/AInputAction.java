package states.actions;

import model.Callback;
import model.User;
import model.telegram.api.TeleFile;
import services.GUI;
import services.TfsService;

import javax.inject.Inject;

/**
 * @author Denis Danilin | denis@danilin.name
 * 27.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class AInputAction {
    public final String name;

    @Inject
    protected TfsService fsService;

    @Inject
    protected GUI gui;

    public AInputAction(final String name) {
        this.name = name;
    }

    public final void ask(final User user) {
        user.setFallback(name);
        askInternal(user);
    }

    public final void onInput(final String input, final User user) {
        user.setFallback(null);
        onInputInternal(input, user);
    }

    abstract void onInputInternal(final String input, final User user);

    abstract void askInternal(final User user);

    public boolean interceptAny() {
        return false;
    }

    public boolean intercepted(final TeleFile file, final String input, final Callback callback, final User user) {
        return false;
    }
}
