package states;

import model.CommandType;
import model.MsgStruct;
import model.opds.OpdsPage;
import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:56
 * tfs ☭ sweat and blood
 */
public class OpdsSearcher extends AState {
    private final String query;
    private int page;

    public OpdsSearcher(final UUID entryId, final String query) {
        this.entryId = entryId;
        this.query = query;
    }

    public OpdsSearcher(final String encoded) {
        final int f = encoded.indexOf(':');
        final int s = encoded.indexOf(':', f + 1);
        entryId = UUID.fromString(encoded.substring(0, f));
        page = s == f + 1 ? 0 : getInt(encoded.substring(f + 1, s));
        query = encoded.substring(s + 1);
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        final OpdsPage opdsPage = user.getOpdsPage(query, page, store);

        final StringBuilder html = new StringBuilder();
        html.append("<i>").append(LangMap.v(LangMap.Value.SEARCH, user.lng)).append(": ").append(query).append("</i><br>");

        final AtomicInteger idxer = new AtomicInteger(1);

        opdsPage.getBooks().forEach(b -> {
            final int idx = idxer.getAndIncrement();
            html.append("<b>").append(b.getTitle()).append("</b> ");

            if (b.getAuthors().size() > 0) {
                html.append(b.getAuthors().first());
                if (b.getAuthors().size() > 1)
                    html.append(" +").append(b.getAuthors().size() - 1);
            }

            if (b.getYear() > 0)
                html.append(" [").append(b.getYear()).append("]<br>");
            if (!isEmpty(b.getContent()))
                html.append("<i>").append(b.getContent()).append("</i><br>");

            if (!isEmpty(b.getFbLink()))
                html.append("FB2: /fb").append(idx).append(" ");


            if (!isEmpty(b.getEpubLink()))
                html.append("EPUB: /ep").append(idx).append(" ");

            html.append("<br><br>");
        });


        final MsgStruct struct = new MsgStruct();
        struct.mode = BotApi.ParseMode.Html;
        struct.body = html.toString();

        struct.kbd = new BotApi.Keyboard();
        idxer.set(0);

        opdsPage.getBooks().stream().filter(b -> !isEmpty(b.getFbRefId()) || !isEmpty(b.getEpubRefId())).forEach(b -> {
            if (!isEmpty(b.getFbRefId()))
                struct.kbd.button(CommandType.openFile.b("[FB] " + b.getTitle(), idxer.getAndIncrement()));
            if (!isEmpty(b.getEpubRefId()))
                struct.kbd.button(CommandType.openFile.b("[EPUB] " + b.getTitle(), idxer.getAndIncrement()));
        });

        if (opdsPage.isHasPrev())
            struct.kbd.button(CommandType.rewind.b());
        if (opdsPage.isHasNext())
            struct.kbd.button(CommandType.forward.b());
        struct.kbd.button(CommandType.cancel.b());

        doSend(struct, user, api);
    }

    @Override
    protected String encode() {
        return entryId + ":" + page + ":" + query;
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.SEARCHED_HELP;
    }
}
