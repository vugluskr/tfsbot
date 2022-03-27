package services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import model.ContentType;
import model.TFile;
import model.User;
import model.opds.Book;
import model.opds.Folder;
import model.opds.Opds;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.Logger;
import play.libs.Json;
import services.OpdsService;
import services.TfsService;
import services.TgApi;
import services.UserService;
import sql.OpdsMapper;
import utils.LangMap;
import utils.TFileFactory;
import utils.Xmls;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:06
 * tfs ☭ sweat and blood
 */
@Singleton
public class OpdsServiceImpl implements OpdsService {
    private static final Logger.ALogger logger = Logger.of(OpdsServiceImpl.class);
    private static final Set<String> beingProcessed = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<String, Set<UserWait>> waitQueues = new ConcurrentHashMap<>();

    @Inject
    private OpdsMapper mapper;

    @Inject
    private TgApi api;

    @Inject
    private TfsService tfs;

    @Inject
    private UserService userService;

    @Override
    public boolean requestOpds(final String url0, final String title, final UUID rootId, final long userId, final String lang) {
        String url = null;
        final URL base;
        try {
            base = new URL(url0);
        } catch (final Exception e) {
            return false;
        }

        try {url = new URL(base.getProtocol(), base.getHost(), base.getPort(), base.getPath()).toExternalForm();} catch (final Exception ignore) {}

        if (url == null)
            return false;

        waitQueues.putIfAbsent(url, new HashSet<>());
        waitQueues.get(url).add(new UserWait(userId, rootId, title, lang));

        if (beingProcessed.contains(url))
            return true;

        if (mapper.opdsExists(url))
            if (mapper.opdsExhausted(url, LocalDateTime.now().minus(6, ChronoUnit.MONTHS)))
                doOpds(url);
            else {
                final String finalUrl = url;
                CompletableFuture.runAsync(() -> sync2users(finalUrl));
            }
        else
            doOpds(url);

        return true;
    }

    private void sync2users(final String url) {
        final Opds opds = mapper.findRawOpdsByUrl(url);

        final Set<UserWait> waits = waitQueues.remove(url);

        if (opds == null || isEmpty(waits))
            return;

        for (final UserWait userWait : waits)
            try {
                TFile dir = tfs.find(userWait.dirId, notNull(userWait.title, opds.getTitle()), userWait.userId);
                if (dir == null)
                    dir = tfs.mk(TFileFactory.dir(notNull(userWait.title, opds.getTitle()), userWait.dirId, userWait.userId));

                childs2fs(dir, mapper.selectOpdsChilds(opds.getId()), Collections.emptyList());

                api.dialog(LangMap.Value.OPDS_DONE, userService.resolveUser(userWait.userId, userWait.lang, ""), userWait.title);
            } catch (final Exception e) {
                logger.error(userWait + " :: " + e.getMessage(), e);
            }

    }

    private void childs2fs(final TFile parent, final List<Folder> folders, final List<Book> books) {
        final Map<Long, UUID> togo = new HashMap<>(0);

        for (final Folder sub : folders)
            if (!isEmpty(sub.getTitle()))
                try {
                    TFile dir = tfs.find(parent.getId(), notNull(sub.getTitle()), parent.getOwner());
                    if (dir == null)
                        dir = tfs.mk(TFileFactory.dir(notNull(sub.getTitle()), parent.getId(), parent.getOwner()));

                    togo.put(sub.getId(), dir.getId());
                } catch (final Exception e) {
                    logger.error("User #" + parent.getOwner() + " Folder #" + sub.getId() + " '" + sub.getTitle() + "' :: " + e.getMessage(), e);
                }

        for (final Book book : books)
            try {
                if (!isEmpty(book.getEpubLink()) && tfs.entryMissed(book.getTitle() + " [EPUB]", parent.getId(), parent.getOwner()))
                    makeTFile(book.getTitle() + " [EPUB]", book.getEpubLink(), parent.getId(), parent.getOwner());

                if (!isEmpty(book.getFbLink()) && tfs.entryMissed(book.getTitle() + " [FB2]", parent.getId(), parent.getOwner()))
                    makeTFile(book.getTitle() + " [FB2]", book.getFbLink(), parent.getId(), parent.getOwner());
            } catch (final Exception e) {
                logger.error("User #" + parent.getOwner() + " Book #" + book.getId() + " '" + book.getTitle() + "' :: " + e.getMessage(), e);
            }

        togo.forEach((folderId, dirId) ->
                childs2fs(
                        tfs.get(dirId, parent.getOwner()),
                        mapper.selectChilds(folderId),
                        mapper.selectBooks(folderId)));
    }

    private void makeTFile(final String title, final String refId, final UUID parentDirId, final long owner) {
        final TFile file = new TFile();
        file.setOwner(owner);
        file.setName(title);
        file.setParentId(parentDirId);
        file.setType(ContentType.DOCUMENT);
        file.setRefId(refId);

        tfs.mk(file);
    }

    private void doOpds(final String url) {
        final URL base;
        try {
            base = new URL(url);
        } catch (final Exception e) {
            logger.error("Cant get OPDS url of '" + url + "': " + e.getMessage(), e);

            return;
        }

        final Function<String, String> urler = path -> {
            if (path != null)
                try {return new URL(base.getProtocol(), base.getHost(), base.getPort(), path).toExternalForm();} catch (final Exception ignore) {}

            return null;
        };

        beingProcessed.add(url);
        CompletableFuture.runAsync(() -> {
            try {
                final Document doc = getXml(url);

                final Opds opds = Xmls.makeOpds(url, doc);

                if (!mapper.opdsExists(url))
                    mapper.insertOpds(opds);

                processChilds(opds.getId(), 0,
                        Xmls.getFolders(doc.getElementsByTagName("entry")),
                        Xmls.getBooks(doc.getElementsByTagName("entry")),
                        urler
                             );

                mapper.updateOpdsUpdated(url, LocalDateTime.now());
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                beingProcessed.remove(url);
            }

            sync2users(url);
        });
    }

    private void processChilds(final int opdsId, final long parentFolderId, final Collection<Folder> subfolders, final Collection<Book> books, final Function<String, String> urler) {
        subfolders.forEach(f -> {
            f.setOpdsId(opdsId);
            f.setParentId(parentFolderId);
            f.setUpdated(LocalDateTime.now());

            if (mapper.folderExists(opdsId, f.getTag()))
                mapper.updateFolder(f);
            else
                mapper.insertFolder(f);
        });

        for (final Book book : books)
            if (mapper.bookMissed(opdsId, book.getTag()))
                try {
                    if (!isEmpty(book.getFbLink()))
                        loadFile(urler.apply(book.getFbLink()), book::setFbLink, ".fb2.zip");
                    if (!isEmpty(book.getEpubLink()))
                        loadFile(urler.apply(book.getEpubLink()), book::setEpubLink, ".epub");

                    mapper.insertBook(book);
                } catch (final Exception e) {
                    logger.error(book + " :: " + e.getMessage(), e);
                }

        for (final Folder f : subfolders)
            try {
                final Document doc = getXml(urler.apply(f.getPath()));

                processChilds(
                        opdsId,
                        f.getId(),
                        Xmls.getFolders(doc.getElementsByTagName("entry")),
                        Xmls.getBooks(doc.getElementsByTagName("entry")),
                        urler);
            } catch (final Exception e) {
                logger.error(urler.apply(f.getPath()) + " :: " + e.getMessage(), e);
            }
    }

    private void loadFile(final String url, final Consumer<String> refConsumer, final String ext) {
        try {
            final File tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()), ext);
            try (final FileOutputStream bos = new FileOutputStream(tmp)) {
                getFile(url, bos);
            }

            final JsonNode rpl = api.upload(tmp).toCompletableFuture().join();

            if (rpl.has("document"))
                refConsumer.accept(rpl.get("document").get("file_id").asText());
            else
                throw new IOException("Failed to upload book: " + Json.stringify(rpl));
        } catch (IOException e) {
            logger.error(url + " :: " + e.getMessage(), e);
        }
    }

    private Document getXml(final String url) {
        final AtomicReference<Document> doc = new AtomicReference<>(null);

        get(url, is -> {
            try {
                doc.set(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new InvalidXmlCharacterFilter(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))))));
            } catch (final Exception e) {
                logger.error(url + " :: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });

        return doc.get();
    }

    private void getFile(final String url, final FileOutputStream os) {
        get(url, is -> {
            try {
                int read;
                while ((read = is.read()) != -1)
                    os.write(read);

                os.flush();
            } catch (final Exception any) {
                logger.error(url + " :: " + any.getMessage(), any);
                throw new RuntimeException(any);
            }
        });
    }

    private void get(final String url, final Consumer<InputStream> os) {
        try {
            final URLConnection cn = new URL(url).openConnection();

            cn.setDoInput(true);
            cn.setDoOutput(true);

            ((HttpURLConnection) cn).setRequestMethod("POST");
            ((HttpURLConnection) cn).setInstanceFollowRedirects(false);
            cn.setUseCaches(false);

            cn.setConnectTimeout(15000);
            cn.setReadTimeout(15000);

            cn.connect();

            final int code = ((HttpURLConnection) cn).getResponseCode();

            if (code / 200 == 1 && code % 200 < 100) {
                os.accept(cn.getInputStream());
                return;
            }

            try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(128); final InputStream is = ((HttpURLConnection) cn).getErrorStream()) {
                int read;
                while ((read = is.read()) != -1)
                    bos.write(read);

                throw new Exception("CH response error message: " + new String(bos.toByteArray(), StandardCharsets.UTF_8));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class UserWait {
        public final long userId;
        public final UUID dirId;
        public final String title, lang;

        public UserWait(final long userId, final UUID dirId, final String title, final String lang) {
            this.userId = userId;
            this.dirId = dirId;
            this.title = title;
            this.lang = lang;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final UserWait userWait = (UserWait) o;
            return userId == userWait.userId && dirId.equals(userWait.dirId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, dirId);
        }

        @Override
        public String toString() {
            return "UserWait{" +
                    "userId=" + userId +
                    ", dirId=" + dirId +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    private static class InvalidXmlCharacterFilter extends FilterReader {
        protected InvalidXmlCharacterFilter(Reader in) {
            super(in);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = super.read(cbuf, off, len);
            if (read == -1) return read;

            for (int i = off; i < off + read; i++) {
                if (!XMLChar.isValid(cbuf[i])) cbuf[i] = '?';
            }
            return read;
        }
    }
}
