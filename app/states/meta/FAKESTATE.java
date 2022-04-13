package states.meta;

import model.MsgStruct;
import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.TUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 17:25
 * tfs ☭ sweat and blood
 */
public final class FAKESTATE implements UserState {
    @Override
    public String save() {
        return null;
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onCommand(final CmdRequest request, final TUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onFile(final FileRequest request, final TUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public UserState onText(final TextRequest request, final TUser user, final BotApi api, final DataStore store) {
        return null;
    }

    @Override
    public void display(final TUser user, final BotApi api, final DataStore store) {
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return null;
    }

    @Override
    public UUID entryId() {
        return null;
    }

    @Override
    public void doSend(final MsgStruct struct, final TUser user, final BotApi api) {
    }

    @Override
    public void doSend(final MsgStruct struct, final TUser user, final BotApi api, final boolean forceNew) {

    }

    @Override
    public CompletionStage<BotApi.Reply> doBookUpload(final MsgStruct struct, final TUser user, final BotApi api) {
        return null;
    }
}
