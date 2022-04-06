package states;

import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 17:25
 * tfs ☭ sweat and blood
 */
public final class BACKSTATE implements UserState {
    @Override
    public String save() {
        return null;
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onCommand(final CmdRequest request, final TgUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onFile(final FileRequest request, final TgUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onText(final TextRequest request, final TgUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return null;
    }
}
