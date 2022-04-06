package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import model.opds.OpdsPage;
import play.libs.Json;
import services.DataStore;
import states.AState;
import states.DirViewer;
import states.UserState;
import utils.Strings;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static utils.TextUtils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 15:52
 * tfs ☭ sweat and blood
 */
public class TgUser {
    public final long id;
    public final String f, l, nik, lng, fio;
    public Consumer<UDbData> asyncSaver;

    private UUID root, bookStore;
    private OpdsSearch opdsSearch;

    private final List<UserState> states = new ArrayList<>();

    public volatile SortedSet<Long> wins;

    public TgUser(final JsonNode node) {
        id = node.get("id").asLong();
        f = node.has("first_name") ? node.get("first_name").asText() : null;
        l = node.has("last_name") ? node.get("last_name").asText() : null;
        nik = node.has("username") ? node.get("username").asText() : null;
        fio = notNull((notNull(f) + " " + notNull(l)), notNull(nik, "u" + id));
        lng = node.has("language_code") ? node.get("language_code").asText() : "en";
    }

    public TgUser(final long id, final UUID root, final String fio) {
        this.id = id;
        this.root = root;
        this.fio = fio;
        f = l = nik = lng = null;
    }

    public void setRoot(final UUID root) {
        this.root = root;
    }

    public UUID getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return "TgUser{" +
                "id=" + id +
                ", f='" + f + '\'' +
                ", l='" + l + '\'' +
                ", nik='" + nik + '\'' +
                ", lng='" + lng + '\'' +
                ", fio='" + fio + '\'' +
                '}';
    }

    public void resolveSaved(final UDbData data) {
        if (data == null)
            return;

        final int p = data.getS1().indexOf(':');
        root = UUID.fromString(p != -1 ? data.getS1().substring(0, p) : data.getS1());
        bookStore = p != -1 ? UUID.fromString(data.getS1().substring(p + 1)) : null;
        if (!isEmpty(data.getS2())) {
            int idx;
            if ((idx = data.getS2().indexOf(Strings.delim)) != -1)
                for (int from = 0; idx != -1; from = idx + 1, idx = data.getS2().indexOf(Strings.delim, from))
                    states.add(AState.resolve(data.getS2().substring(from, idx), this));
            else
                states.add(AState.resolve(data.getS2(), this));
        }

        if (!isEmpty(data.getS3())) {
            wins = new TreeSet<>();
            final List<Integer> pos = new ArrayList<>();
            final String q = data.getS3();

            for (int i = 0; i < q.length(); i++)
                if (q.charAt(i) == ',')
                    pos.add(i);

            if (pos.isEmpty())
                wins.add(getLong(q));
            else {
                pos.add(0, -1);
                pos.add(q.length());

                for (int i = 0; i < pos.size() - 1; i++)
                    wins.add(getLong(q.substring(pos.get(i) + 1, pos.get(i + 1))));
            }
        }

        if (!isEmpty(data.getS4()))
            opdsSearch = Json.fromJson(Json.parse(data.getS4()), OpdsSearch.class);
    }

    public UUID getBookStore() {
        return bookStore;
    }

    public void setBookStore(final UUID bookStore) {
        this.bookStore = bookStore;
    }

    public UDbData encodeToSave() {
        final UDbData data = new UDbData();
        data.setId(id);
        data.setS1(root + (bookStore == null ? "" : ":" + bookStore));
        data.setS2(states.stream().map(UserState::save).collect(Collectors.joining(Strings.delim)) + Strings.delim);

        if (!isEmpty(wins))
            data.setS3(wins.stream().map(String::valueOf).collect(Collectors.joining(",")));

        if (opdsSearch != null)
            data.setS4(Json.toJson(opdsSearch).toString());

        return data;
    }

    public void addWin(final long msgId) {
        if (wins == null)
            wins = new TreeSet<>();

        wins.add(msgId);
    }

    public UserState state() {
        return states.isEmpty() ? null : states.get(states.size() - 1);
    }

    public void backHistory() {
        states.remove(states.size() - 1);
        if (states.isEmpty())
            states.add(new DirViewer(root));
    }

    public void addState(final UserState state) {
        states.add(state);
    }

    public void interactionDone() {
        if (asyncSaver != null)
            asyncSaver.accept(encodeToSave());
    }

    public void resetState() {
        states.clear();
        states.add(new DirViewer(root));
    }

    public void clearHistoryTail(final UUID entryId) {
        for (int i = states.size() - 1; i >= 0; i--)
            if (states.get(i).entryId().equals(entryId))
                states.remove(i);
            else
                break;
    }

    public OpdsPage getOpdsPage(final String query, final int pageNum, final DataStore store) {
        OpdsPage page;

        if (opdsSearch == null || !opdsSearch.query.equals(query) || pageNum >= opdsSearch.pages.size())
            page = null;
        else
            page = opdsSearch.pages.get(pageNum);

        if (page == null) {
            page = store.doOpdsSearch(query, pageNum);
            opdsSearch.pages.add(page);
        }

        return page;
    }

    private static class OpdsSearch {
        private String query;
        private List<OpdsPage> pages;

        public String getQuery() {
            return query;
        }

        public void setQuery(final String query) {
            this.query = query;
        }

        public List<OpdsPage> getPages() {
            return pages;
        }

        public void setPages(final List<OpdsPage> pages) {
            this.pages = pages;
        }
    }

}
