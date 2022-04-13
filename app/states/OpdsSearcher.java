package states;

import com.fasterxml.jackson.databind.JsonNode;
import model.CommandType;
import model.MsgStruct;
import model.TFile;
import model.opds.OpdsBook;
import model.opds.OpdsPage;
import model.TBook;
import model.request.CallbackRequest;
import model.request.CmdRequest;
import model.TUser;
import play.Logger;
import play.libs.Json;
import services.BotApi;
import services.DataStore;
import states.meta.AState;
import states.meta.UserState;
import utils.LangMap;
import utils.Strings;
import utils.TFileFactory;
import utils.TextUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static utils.TextUtils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:56
 * tfs ☭ sweat and blood
 */
public class OpdsSearcher extends AState {
    private static final Logger.ALogger logger = Logger.of(OpdsSearcher.class);
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
    protected UserState voidOnCallback(final CallbackRequest request, final TUser user, final BotApi api, final DataStore store) {
        switch (request.getCommand().type) {
            case rewind:
                page--;
                break;
            case forward:
                page++;
                break;
            case cancelSearch:
                user.resetOpds();
                break;
            case openDir:
            case openFile:
                final AtomicInteger idx = new AtomicInteger(0);
                final OpdsPage opdsPage = user.getOpdsPage(query, page, store);
                final int match = request.getCommand().elementIdx;

                opdsPage.getBooks().stream().filter(b -> !isEmpty(b.getFbRefId()) || !isEmpty(b.getEpubRefId())).forEach(b -> {
                    final int bidx = idx.getAndIncrement();

                    if (match == bidx) {
                        user.addState(new FileViewer(UUID.fromString(request.getCommand().type == CommandType.openFile ? b.getFbRefId() : b.getEpubRefId())));

                        idx.set(-1);
                    }
                });
                break;
        }

        return null;
    }

    @Override
    public UserState onCommand(final CmdRequest request, final TUser user, final BotApi api, final DataStore store) {
        switch (request.getCmd()) {
            case FbBook:
            case EpubBook:
                final OpdsPage opdsPage = user.getOpdsPage(query, page, store);
                try {
                    final OpdsBook book = opdsPage.getBooks().get(getInt(request.getArg()) - 1);
                    TBook db = store.getStoredBook(book, request.getCmd() == CmdRequest.Cmd.FbBook, request.getCmd() == CmdRequest.Cmd.EpubBook);

                    if (db == null)
                        db = store.loadBookFile(book, request.getCmd() == CmdRequest.Cmd.FbBook, request.getCmd() == CmdRequest.Cmd.EpubBook, api);

                    if (db == null) {
                        final MsgStruct struct = new MsgStruct();
                        struct.body = escapeMd(LangMap.v(LangMap.Value.OPDS_FAILED, user.lng));
                        struct.kbd = BotApi.voidKbd;
                        struct.mode = BotApi.ParseMode.Md2;

                        doSend(struct, user, api, true);
                        return _hold;
                    }

                    if (!isEmpty(db.refId))
                        return new FileViewer(UUID.fromString(db.refId));

                    final SortedSet<String> authors = isEmpty(db.authors) ? new TreeSet<>() : new TreeSet<>(Arrays.asList(db.authors.split(Pattern.quote(Strings.delim))));
                    final Map<String, String> genres = isEmpty(db.genres) ? Collections.emptyMap() :
                            Arrays.stream(db.genres.split(Pattern.quote(Strings.delim)))
                                    .collect(Collectors.toMap(id -> id, store::getOpdsGenreName));

                    final Set<TFile> dirs = store.mkBookDirs(book.getTitle(), genres.keySet(), authors, user);

                    final String nameWithAuthors = db.title +
                            (isEmpty(authors) ? "" : " [" + authors.stream().map(TextUtils::fio2shortName).collect(Collectors.joining(", ")) + "]") +
                            (book.getYear() > 0 ? " (" + book.getYear() + ")" : "");

                    final String simpleName = db.title + (book.getYear() > 0 ? " (" + book.getYear() + ")" : "");

                    final String nameWithGenres = db.title +
                            (isEmpty(genres) ? "" : " [" + String.join(", ", new TreeSet<>(genres.values())) + "]") +
                            (book.getYear() > 0 ? " (" + book.getYear() + ")" : "");

                    final TFile abc = dirs.stream().filter(TFile::isAbc).findFirst().orElseThrow(() -> new RuntimeException("No abc dir"));

                    final TFile base = store.mkIfMissed(TFileFactory.bookFile(simpleName, abc.getId(), user.id, null));
                    db.refId = base.getId().toString();
                    store.insertBook(db);

                    final MsgStruct struct = new MsgStruct();
                    struct.rawFile = db.file;
                    struct.file = base;
                    struct.kbd = new BotApi.Keyboard();
                    struct.kbd.button(CommandType.goBack.b());
                    struct.kbd.button(CommandType.lock.b());
                    struct.kbd.button(CommandType.share.b());
                    struct.kbd.button(CommandType.rename.b(), CommandType.drop.b());
                    struct.mode = BotApi.ParseMode.Md2;
                    struct.caption = escapeMd(base.getPath());

                    CompletableFuture.runAsync(() -> doBookUpload(struct, user, api)
                            .thenAccept(reply -> {
                                if (!reply.isOk()) {
                                    logger.error("Failed to upload book: " + reply.getDesc());
                                    return;
                                }

                                final JsonNode n = Json.parse(reply.getRawData());
                                base.setRefId(n.get("result").get("document").get("file_id").asText());
                                store.updateEntryRef(base);

                                user.addState(new FileViewer(base.getId()));
                                user.state().display(user, api, store);

                                dirs.stream().filter(d -> !d.isAbc() && d.isBookDir()).forEach(d -> store.mkIfMissed(TFileFactory.softLink(d.isAuthors() ? nameWithGenres : nameWithAuthors,
                                        base.getId().toString(), d.getId(), user.id)));
                            }));

                    return _hold;
                } catch (final Exception e) {
                    logger.error(e.getMessage(), e);
                }
                break;
        }

        return null;
    }

    @Override
    public void display(final TUser user, final BotApi api, final DataStore store) {
        final OpdsPage opdsPage = user.getOpdsPage(query, page, store);

        final StringBuilder md = new StringBuilder();
        md.append("_").append(LangMap.v(LangMap.Value.SEARCH, user.lng)).append(": ").append(query).append("_\n");

        final AtomicInteger idxer = new AtomicInteger(1);

        for (final OpdsBook b : opdsPage.getBooks()) {
            final int idx = idxer.getAndIncrement();
            md.append("*").append(escapeMd(b.getTitle())).append("* ");

            if (b.getAuthors().size() > 0) {
                md.append(escapeMd(b.getAuthors().first()));
                if (b.getAuthors().size() > 1)
                    md.append(" \\+").append(b.getAuthors().size() - 1);
            }

            if (b.getYear() > 0)
                md.append(" \\[").append(b.getYear()).append("\\]");
            md.append("\n");


            if (!isEmpty(b.getFbLink()))
                md.append("FB2: /fb").append(idx).append(" ");


            if (!isEmpty(b.getEpubLink()))
                md.append("EPUB: /ep").append(idx).append(" ");

            md.append("\n\n");
        }


        final MsgStruct struct = new MsgStruct();
        struct.mode = BotApi.ParseMode.Md2;
        struct.body = md.toString();

        struct.kbd = new BotApi.Keyboard();
        idxer.set(0);

        opdsPage.getBooks().stream().filter(b -> !isEmpty(b.getFbRefId()) || !isEmpty(b.getEpubRefId())).forEach(b -> {
            final int idx = idxer.getAndIncrement();
            if (!isEmpty(b.getFbRefId()))
                struct.kbd.button(CommandType.openFile.b("[FB] " + b.getTitle(), idx));
            if (!isEmpty(b.getEpubRefId()))
                struct.kbd.button(CommandType.openDir.b("[EPUB] " + b.getTitle(), idx));
        });

        if (opdsPage.isHasPrev())
            struct.kbd.button(CommandType.rewind.b());
        if (opdsPage.isHasNext())
            struct.kbd.button(CommandType.forward.b());
        struct.kbd.button(CommandType.cancelSearch.b());

        doSend(struct, user, api);
    }

    @Override
    protected String encode() {
        return entryId + ":" + page + ":" + query;
    }

    @Override
    public LangMap.Value helpValue(final TUser user) {
        return LangMap.Value.SEARCHED_HELP;
    }
}
