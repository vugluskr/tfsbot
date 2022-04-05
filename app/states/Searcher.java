package states;

import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.request.CallbackRequest;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.UUID;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 16:22
 * tfs ☭ sweat and blood
 */
public class Searcher extends AState {
    private UUID startPointId;
    private String query;
    private int offset;

    public Searcher(final UUID entryId, final String query) {
        this.startPointId = entryId;
        this.query = query;
    }

    public Searcher(final String encoded) {
        for (int i = 0, from = 0, idx = 0; i < encoded.length(); i++)
            if (encoded.charAt(i) == ':') {
                final String s = encoded.substring(from, i);
                from = i + 1;
                switch (idx++) {
                    case 0:
                        startPointId = UUID.fromString(s);
                        break;
                    case 1:
                        offset = getInt(s);
                        query = encoded.substring(i + 1);
                        return;
                }
            }
    }

    @Override
    public UserState onCallback(final CallbackRequest request, final TgUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case openDir:
                return new DirViewer(store.searchFolder(startPointId, query, offset, 10, user.id).get(request.getCommand().elementIdx));
            case openFile:
                return new FileViewer(store.searchFolder(startPointId, query, offset, 10, user.id).get(request.getCommand().elementIdx));
            case openLabel:
                return new LabelViewer(store.searchFolder(startPointId, query, offset, 10, user.id).get(request.getCommand().elementIdx));
            case rewind:
                offset -= 10;
                break;
            case forward:
                offset += 10;
                break;
        }

        return null;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final TFile entry = store.getEntry(startPointId, user.id);

        final MsgStruct struct = new MsgStruct();

        struct.kbd = new BotApi.Keyboard();
        struct.kbd.button(CommandType.goBack.b());

        final int count = store.countSearch(startPointId, query, user.id);

        final StringBuilder body = new StringBuilder(0);
        body.append(escapeMd(v(LangMap.Value.SEARCHED, user, query, entry.getPath())));

        if (count == 0)
            body.append("\n_").append(v(LangMap.Value.NO_RESULTS, user)).append("_");
        else
            body.append("\n_").append(escapeMd(v(LangMap.Value.RESULTS_FOUND, user))).append("_");

        struct.body = body.toString();

        pagedList(store.searchFolder(startPointId, query, offset, 10, user.id), count, offset, struct);

        struct.mode = BotApi.ParseMode.Md2;

        doSend(struct, user, api);
    }

    @Override
    public String encode() {
        return startPointId + ":" + offset + ":" + query;
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.SEARCHED_HELP;
    }
}
