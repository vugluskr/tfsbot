package states.meta;

import model.MsgStruct;
import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.request.FileRequest;
import model.request.TextRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:25
 * tfs ☭ sweat and blood
 */
public interface UserState {
    UUID entryId();

    void display(TgUser user, BotApi api, DataStore store);

    LangMap.Value helpValue(TgUser user);

    String save();

    void doSend(MsgStruct struct, TgUser user, BotApi api);

    void doSend(MsgStruct struct, TgUser user, BotApi api, boolean forceNew);

    CompletionStage<BotApi.Reply> doBookUpload(MsgStruct struct, TgUser user, BotApi api);

    UserState onCallback(CallbackRequest request, TgUser user, BotApi api, DataStore store);

    UserState onCommand(CmdRequest request, TgUser user, BotApi api, DataStore store);

    UserState onFile(FileRequest request, TgUser user, BotApi api, DataStore store);

    UserState onText(TextRequest request, TgUser user, BotApi api, DataStore store);
}
