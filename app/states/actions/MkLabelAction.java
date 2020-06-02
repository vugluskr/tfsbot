package states.actions;

import model.User;
import utils.LangMap;
import utils.TFileFactory;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class MkLabelAction extends AInputAction {
    public static final String NAME = MkLabelAction.class.getSimpleName();

    public MkLabelAction() {
        super(NAME);
    }

    @Override
    void onInputInternal(final String input, final User user) {
        if (!user.current.isRw()) {
            gui.notify(LangMap.Value.NOT_ALLOWED, user, input);
            return;
        }

        if (fsService.entryExist(input, user))
            gui.notify(LangMap.Value.CANT_MKLBL, user, input);
        else
            fsService.mk(TFileFactory.label(input, user.current.getId(), user.getId()));
    }

    @Override
    void askInternal(final User user) {
        gui.dialog(LangMap.Value.TYPE_LABEL, user);
    }
}
