package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Command;
import model.CommandType;
import model.TFile;
import play.Logger;
import services.TfsService;
import services.TgApi;
import services.UserService;
import utils.LangMap;

import java.util.List;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.06.2020
 * tfs ☭ sweat and blood
 */
public class Searcher extends APager<TFile> implements InputSink {
    private static final Logger.ALogger logger = Logger.of(Searcher.class);

    public String query;

    private volatile String path;

    public Searcher(final TgApi api, final TfsService tfs, final UserService us, final JsonNode node) {
        super(api, tfs, us, node);

        query = node != null && node.has("query") ? node.get("query").asText() : "";
    }

    @Override
    public void onCallback(final Command command) {
        if (notPagerCall(command))
            switch (command.type) {
                case openSearchedDir:
                    us.morphTo(DirViewer.class, user).doView(tfs.getSearchEntry(query, command.elementIdx, this));
                    break;
                case openSearchedFile:
                    us.morphTo(FileViewer.class, user).doView(tfs.getSearchEntry(query, command.elementIdx, this));
                    break;
                case openSearchedLabel:
                    us.morphTo(LabelViewer.class, user).doView(tfs.getSearchEntry(query, command.elementIdx, this));
                    break;
                case cancelSearch:
                    final TFile entry = tfs.get(entryId, user);
                    final Class<? extends Role> role = entry.isDir() ? DirViewer.class : entry.isLabel() ? LabelViewer.class : FileViewer.class;

                    us.morphTo(role, user).doView();
                    break;
                case Void:
                    user.doView();
                    break;
                default:
                    logger.info("Нет обработчика для '" + command.type.name() + "'");
                    us.reset(user);
                    user.doView();
                    break;
            }
    }

    @Override
    protected TgApi.Keyboard initKeyboard() {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();
        kbd.button(CommandType.cancelSearch.b());

        return kbd;
    }

    @Override
    protected String initBody(final boolean noElements) {
        final StringBuilder body = new StringBuilder(16);
        body.append(escapeMd(v(LangMap.Value.SEARCHED, user, query, path)));

        if (noElements)
            body.append("\n_").append(v(LangMap.Value.NO_RESULTS, user)).append("_");
        else
            body.append("\n_").append(escapeMd(v(LangMap.Value.RESULTS_FOUND, user))).append("_");

        return body.toString();
    }

    @Override
    protected int prepareCountScope() {
        path = notNull(tfs.get(entryId, user).getPath(), "/");

        return tfs.countSearch(query, entryId, user);
    }

    @Override
    protected TgApi.Button toButton(final TFile element, final int withIdx) {
        return element.toSearchedButton(path.length(), withIdx);
    }

    @Override
    protected List<TFile> selectScope(final int offset, final int limit) {
        return tfs.search(this);
    }

    @Override
    protected String offName() {
        return "search_offset";
    }

    @Override
    public JsonNode dump() {
        final ObjectNode node = rootDump();

        node.put("query", query);

        return node;
    }

    @Override
    public LangMap.Value helpValue() {
        return LangMap.Value.SEARCHED_HELP;
    }

    @Override
    public void onInput(final String input) {
        initSearch(input);
    }

    public void initSearch(final String input) {
        query = input;
        offset = 0;

        doView();
    }
}
