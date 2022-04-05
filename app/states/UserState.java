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
 * 04.04.2022 15:25
 * tfs ☭ sweat and blood
 */
public interface UserState {

    String encode();

    void display(TgUser user, BotApi api, DataStore store);

    LangMap.Value helpValue(TgUser user);

    String save();

    UserState onCallback(CallbackRequest request, TgUser user, BotApi api, DataStore store);

    UserState onCommand(CmdRequest request, TgUser user, BotApi api, DataStore store);

    UserState onFile(FileRequest request, TgUser user, BotApi api, DataStore store);

    UserState onText(TextRequest request, TgUser user, BotApi api, DataStore store);
}
