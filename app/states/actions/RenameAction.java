package states.actions;

import model.TFile;
import model.User;
import utils.LangMap;

import java.nio.file.Paths;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class RenameAction extends AInputAction {
    public static final String NAME = RenameAction.class.getSimpleName();

    public RenameAction() {
        super(NAME);
    }

    @Override
    void onInputInternal(final String input, final User user) {
        if (fsService.entryExist(input, user)) {
            gui.notify(LangMap.Value.CANT_RN_TO, user, input);
            user.skipTail = true;
            return;
        }

        final TFile entry;
        if (!user.selection.isEmpty() && !(entry = user.selection.get(0)).getName().equals(input)) {
            if (!entry.isRw()) {
                gui.notify(LangMap.Value.NOT_ALLOWED_THIS, user, input);
                user.skipTail = true;
                return;
            }

            entry.setName(input);
            entry.setPath(Paths.get(entry.getPath()).getParent().resolve(input).toString());
            fsService.updateMeta(entry, user);
        }
    }

    @Override
    void askInternal(final User user) {
        final TFile entry;

        if (user.selection.isEmpty() || (entry = user.selection.get(0)) == null)
            return;

        gui.dialog(LangMap.Value.TYPE_RENAME, user, entry.getName());
    }
}
