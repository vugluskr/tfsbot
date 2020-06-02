package states.actions;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.api.ContactRef;
import model.telegram.api.TeleFile;
import services.MemStore;
import utils.LangMap;

import javax.inject.Inject;

import java.util.concurrent.CompletableFuture;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 28.05.2020
 * tfs ☭ sweat and blood
 */
public class MkGrantAction extends AInputAction {
    public static final String NAME = MkGrantAction.class.getSimpleName();

    @Inject
    private MemStore memStore;

    public MkGrantAction() {
        super(NAME);
    }

    @Override
    void onInputInternal(final String input, final User user) { }

    @Override
    void askInternal(final User user) {
        final TFile entry = user.selection.get(0);
        gui.notify(entry.isDir() ? LangMap.Value.SEND_CONTACT_DIR : LangMap.Value.SEND_CONTACT_FILE, user, entry.getPath());
    }

    @Override
    public boolean interceptAny() {
        return true;
    }

    @Override
    public boolean intercepted(final TeleFile file, final String input, final Callback cb, final User user) {
        user.setFallback(null);

        if (!(file instanceof ContactRef)) {
            user.setFallback(null);
            return false;
        }

        ((ContactRef) file).setId(((ContactRef) file).getUserId());
        CompletableFuture.runAsync(() -> memStore.getUser((ContactRef) file).thenAccept(target -> {
            final TFile entry = user.selection.get(0);

            if (fsService.shareExist(entry.getId(), target.getId(), user)) {
                gui.notify(LangMap.Value.CANT_GRANT, user, ((ContactRef) file).name());
                user.skipTail = true;
            } else {
                fsService.makeShare(((ContactRef) file).name(), user, entry.getId(), target.getId(), notNull(target.getLang(), "en"));
                if (!entry.isShared()) {
                    entry.setShared();
                    fsService.updateMeta(entry, user);
                }
            }
        }));

        return true;
    }
}
