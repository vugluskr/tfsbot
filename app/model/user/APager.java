package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Command;
import model.CommandType;
import model.ParseMode;
import services.TfsService;
import services.TgApi;
import services.UserService;

import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.06.2020
 * tfs ☭ sweat and blood
 */
public abstract class APager<T> extends ARole implements CallbackSink {
    public int offset;

    public APager(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);

        offset = node != null && node.has(offName()) ? node.get(offName()).asInt() : 0;
    }

    protected boolean notPagerCall(final Command command) {
        switch (command.type) {
            case rewind:
            case forward:
                offset += command.type == CommandType.rewind ? -10 : 10;
                doView();
                return false;
            default:
                return true;
        }
    }

    @Override
    protected ObjectNode rootDump() {
        final ObjectNode node = super.rootDump();

        node.put(offName(), offset);

        return node;
    }

    public final void scopeChanged() {
        offset = 0;
    }

    @Override
    public final void doView() {
        if (!isViewAllowed())
            return;

        final int count = prepareCountScope();
        final TgApi.Keyboard kbd = initKeyboard();

        final List<T> scope = selectScope(offset, 10);

        for (int i = 0; i < scope.size(); i++)
            kbd.newLine().button(toButton(scope.get(i), i));

        if (offset > 0 || count > 10) {
            kbd.newLine();

            if (offset > 0)
                kbd.button(CommandType.rewind.b());
            if ((count - offset) > 10)
                kbd.button(CommandType.forward.b());
        }

        api.sendContent(null, initBody(count == 0), ParseMode.md2, kbd, user);
    }

    protected boolean isViewAllowed() {
        return true;
    }

    protected abstract int prepareCountScope();

    protected abstract TgApi.Button toButton(final T element, final int withIdx);

    protected abstract List<T> selectScope(final int offset, final int limit);

    protected abstract String initBody(final boolean noElements);

    protected abstract TgApi.Keyboard initKeyboard();

    protected String offName() { return "offset"; }
}
