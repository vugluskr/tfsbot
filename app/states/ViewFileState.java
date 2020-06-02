package states;

import model.Callback;
import model.User;
import states.actions.RenameAction;
import utils.LangMap;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class ViewFileState extends AState {
    public static final String NAME = ViewFileState.class.getSimpleName();

    public ViewFileState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case rename:
                doAction(RenameAction.NAME, user);
                break;
            case move:
                doView(MoveState.NAME, user);
                break;
            case drop:
                fsService.rmSelected(user);
                reply.reply(LangMap.Value.DELETED);
                doView(LsState.NAME, user);
                break;
            default:
                doView(LsState.NAME, user);
                break;
        }
    }

    @Override
    protected void handle(final User user) {
        gui.makeFileDialog(user.selection.get(0), user);
    }
}
