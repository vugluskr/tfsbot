package services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import model.ContentType;
import model.TFile;
import model.opds.Book;
import model.opds.Folder;
import model.opds.Opds;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;
import play.libs.Json;
import services.OpdsService;
import services.TfsService;
import services.TgApi;
import sql.OpdsMapper;
import utils.TFileFactory;
import utils.Xmls;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

import static utils.TextUtils.*;

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

    @Override
    public void requestOpds(final String url, final String title, final UUID rootId, final long userId) {
        waitQueues.putIfAbsent(url, new HashSet<>());
        waitQueues.get(url).add(new UserWait(userId, rootId, title));

        if (beingProcessed.contains(url))
            return;

        if (mapper.opdsExists(url))
            if (mapper.opdsExhausted(url, LocalDateTime.now().minus(6, ChronoUnit.MONTHS))) {
                beingProcessed.add(url);
                doOpds(url, title);
            } else
                sync2users(url);
        else
            doOpds(url, title);
    }

    private void sync2users(final String url) {
        final Opds opds = mapper.findRawOpdsByUrl(url);

        final Set<UserWait> waits = waitQueues.remove(url);

        waits.forEach(uw -> {
            final UUID parent = uw.dirId;

            opds.childs.forEach(f -> fs(f, uw, parent));
        });
    }

    private void fs(final Folder f, final UserWait uw, final UUID rootId) {
        final TFile dir = tfs.mk(TFileFactory.dir(f.getTitle(), rootId, uw.userId));

        mapper.selectChilds(f.getId()).forEach(sub -> fs(sub, uw, dir.getId()));
        mapper.selectBooks(f.getId()).forEach(b -> {
            final TFile file = new TFile();
            file.setOwner(uw.userId);
            file.setId(generateUuid());
            file.setName(b.getTitle());
            file.setParentId(dir.getId());
            file.setType(ContentType.DOCUMENT);

            tfs.mk(file);
        });
    }

    private void doOpds(final String url, final String title) {
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

        CompletableFuture.runAsync(() -> {
            try {
                final Document doc = getXml(base.toExternalForm());

                final Opds opds = Xmls.makeOpds(url, title, doc);

                opds.childs.addAll(Xmls.getFolders(doc.getElementsByTagName("entry")));

                if (isEmpty(opds.childs)) {
                    logger.warn("OPDS " + url + " doesnt have folders");
                    return;
                }

                mapper.insertOpds(opds);

                for (final Folder f : opds.childs)
                    saveProcessFolder(f, opds.getId(), urler);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }

            sync2users(url);
        });
    }

    private void saveProcessFolder(final Folder f, final int opdsId, final Function<String, String> urler) throws ParserConfigurationException, IOException, SAXException {
        f.setOpdsId(opdsId);
        if (f.getId() > 0)
            mapper.updateFolder(f);
        else
            mapper.insertFolder(f);

        logger.debug("Got folder: " + f);

        handleFolder(f, opdsId, urler.apply(f.getPath()), urler);
    }

    private void handleFolder(final Folder f, final int opdsId, final String url, final Function<String, String> urler) throws ParserConfigurationException, IOException, SAXException {
        if (url == null)
            return;

        final Document doc = getXml(url);
        f.mergeChilds(Xmls.getFolders(doc.getElementsByTagName("entry")));
        f.mergeBooks(Xmls.getBooks(doc.getElementsByTagName("entry")));

        final NodeList links = doc.getElementsByTagName("link");

        for (int i = 0; i < links.getLength(); i++) {
            final Node linkNode = links.item(i);
            final NamedNodeMap attrs = linkNode.getAttributes();

            String href = "";
            boolean next = false;

            for (int j = 0; j < attrs.getLength(); j++) {
                final Node a = attrs.item(j);

                if (a.getNodeName().equals("rel"))
                    next = a.getNodeValue().equals("next");
                else if (a.getNodeName().equals("href"))
                    href = a.getNodeValue();
            }

            if (next && !isEmpty(href)) {
                handleFolder(f, opdsId, urler.apply(href), urler);
                return;
            }
        }

        for (final Book b : f.books)
            if (!isEmpty(b.getFbLink()))
                try {
                    final File tmp = File.createTempFile(b.getId() + "_" + System.currentTimeMillis(), ".fb2");
                    try (final FileOutputStream bos = new FileOutputStream(tmp)) {
                        getFile(urler.apply(b.getFbLink()), bos);
                    }

                    final JsonNode rpl = api.upload(tmp).toCompletableFuture().join();

                    if (rpl.has("document")) {
                        b.setRefId(rpl.get("document").get("file_id").asText());
                        if (b.getId() > 0)
                            mapper.updateBook(b);
                        else
                            mapper.insertBook(b);

                        logger.debug("Got book: " + b);
                    } else
                        throw new IOException("Failed to upload book: " + Json.stringify(rpl));
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }

        for (final Folder sub : f.childs)
            saveProcessFolder(sub, opdsId, urler);
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
        public final String title;

        public UserWait(final long userId, final UUID dirId, final String title) {
            this.userId = userId;
            this.dirId = dirId;
            this.title = title;
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
