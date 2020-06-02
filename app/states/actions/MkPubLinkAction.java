package states.actions;

import model.Callback;
import model.Share;
import model.TFile;
import model.User;
import model.telegram.api.TeleFile;
import utils.LangMap;
import utils.TextUtils;

import static utils.Strings.Callback.ok;

/**
 * @author Denis Danilin | denis@danilin.name
 * 28.05.2020
 * tfs ☭ sweat and blood
 */
public class MkPubLinkAction extends AInputAction {
    public static final String NAME = MkPubLinkAction.class.getSimpleName();

    public MkPubLinkAction() {
        super(NAME);
    }

    @Override
    void onInputInternal(final String input, final User user) { }

    @Override
    public boolean interceptAny() {
        return true;
    }

    @Override
    public boolean intercepted(final TeleFile file, final String input, final Callback cb, final User user) {
        user.setFallback(null);

        if (cb == null)
            return false;

        if (cb.type().equals(ok)) {
            final TFile entry = user.selection.get(0);

            if (fsService.isGlobalShareMissed(entry.getId(), user)) {
                fsService.makeShare(entry.getName(), user, entry.getId(), 0, null);

                if (!entry.isShared()) {
                    entry.setShared();
                    fsService.updateMeta(entry, user);
                }
            } else {
                fsService.dropGlobalShareByEntry(entry.getId(), user);
                if (fsService.noSharesExist(entry.getId(), user)) {
                    entry.setUnshared();
                    fsService.updateMeta(entry, user);
                }
            }
        }

        return true;
    }

    @Override
    void askInternal(final User user) {
        final TFile entry = user.selection.get(0);
        final Share share = fsService.listShares(entry.getId(), user).stream().filter(Share::isGlobal).findAny().orElse(null);

        gui.yesNoPrompt(share == null
                ? (entry.isDir() ? LangMap.Value.CREATE_PUBLINK_DIR : LangMap.Value.CREATE_PUBLINK_FILE)
                : (entry.isDir() ? LangMap.Value.DROP_PUBLINK_DIR : LangMap.Value.DROP_PUBLINK_FILE),
                user, TextUtils.escapeMd(entry.getPath()));
    }
}
