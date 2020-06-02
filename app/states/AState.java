package states;

import model.Callback;
import model.User;
import services.GUI;
import services.TfsService;
import services.TgApi;
import states.actions.AInputAction;
import utils.LangMap;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */

public abstract class AState {
    public final String name;

    @Inject
    protected TgApi tgApi;

    @Inject
    protected TfsService fsService;

    @Inject
    protected GUI gui;

    private final AtomicReference<Function<String, ? extends AState>> viewResolve;
    private final AtomicReference<Function<String, ? extends AInputAction>> actionResolve;

    public AState(final String name) {
        this.name = name;
        viewResolve = new AtomicReference<>(null);
        actionResolve = new AtomicReference<>(null);
    }

    public final void onCallback(final Callback callback, final User user, final Function<String, ? extends AState> resolver,
                                 final Function<String, ? extends AInputAction> actionResolver) {
        if (resolver != null)
            viewResolve.compareAndSet(null, resolver);

        if (actionResolver != null)
            actionResolve.compareAndSet(null, actionResolver);

        final AtomicBoolean applied = new AtomicBoolean(false);
        final CallReply reply = (s, args) -> CompletableFuture.runAsync(() -> {
            if (applied.compareAndSet(false, true))
                tgApi.sendCallbackAnswer(s, callback.id, false, 0, user, args);
        });

        handleCallback(callback, user, reply);
        reply.reply(LangMap.Value.None);
    }

    public final void doView(final User user, final Function<String, ? extends AState> resolver, final Function<String, ? extends AInputAction> actionResolver) {
        if (resolver != null) viewResolve.compareAndSet(null, resolver);
        if (actionResolver != null) actionResolve.compareAndSet(null, actionResolver);

        user.setState(name);
        handle(user);
    }

    protected final void doView(final String state, final User user) {
        user.setState(state);
        viewResolve.get().apply(state).handle(user);
    }

    protected final void doAction(final String action, final User user) {
        actionResolve.get().apply(action).ask(user);
    }

    protected abstract void handle(final User user);

    protected abstract void handleCallback(final Callback callback, final User user, final CallReply reply);

    @FunctionalInterface
    public interface CallReply {
        void reply(LangMap.Value v, Object... args);
    }
}
