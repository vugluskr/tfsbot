package services.impl;

import com.sun.org.apache.xerces.internal.util.XMLChar;
import model.TFile;
import model.opds.Book;
import model.opds.OpdsPage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import play.Logger;
import services.OpdsSearch;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
public class OpdsSearchFlibusta implements OpdsSearch {
    private static final Logger.ALogger logger = Logger.of(OpdsSearch.class);
    private static final String searchUrl = "https://flibusta.is/opds/search?searchType=books&searchTerm=";

    @Override
    public OpdsPage search(final String query, final int page) {
        final Document doc = getXml(searchUrl + URLEncoder.encode(query, "UTF-8"));

        final NodeList entries = doc.getElementsByTagName("entry");
        final OpdsPage p = new OpdsPage();
        p.setBooks(new ArrayList<>(0));

        for (int i = 0; i < entries.getLength(); i++) {
            final Node el = entries.item(i);
            final Book b = new Book();

            for (int j = 0; j < el.getChildNodes().getLength(); j++) {
                final Node child = el.getChildNodes().item(j);

                switch (child.getNodeName()) {
                    case "content":
                        b.setContent(child.getTextContent());
                        break;
                    case "author":
                        for (int z = 0; z < child.getChildNodes().getLength(); z++)
                            if (child.getChildNodes().item(z).getNodeName().equals("name")) {
                                b.getAuthors().add(child.getChildNodes().item(z).getTextContent());
                                break;
                            }

                        break;
                    case "dc:issued":
                        b.setYear(getInt(child.getTextContent()));
                        break;
                    case "id":
                        b.setId(child.getTextContent());
                        break;
                    case "title":
                        b.setTitle(child.getTextContent());
                        break;
                    case "link":
                        String type = "", href = "", rel = "";

                        for (int k = 0; k < child.getAttributes().getLength(); k++) {
                            final Node a = child.getAttributes().item(k);

                            switch (a.getNodeName()) {
                                case "type":
                                    type = a.getNodeValue();
                                    break;
                                case "href":
                                    href = a.getNodeValue();
                                    break;
                                case "rel":
                                    rel = a.getNodeValue();
                                    break;
                            }
                        }

                        if (rel.equals("http://opds-spec.org/acquisition/open-access")) {
                            if (type.contains("application/fb2"))
                                b.setFbLink(href);
                            else if (type.contains("application/epub"))
                                b.setEpubLink(href);
                        }

                        break;
                }
            }

            if (!isEmpty(b.getTitle()) && (!isEmpty(b.getFbLink()) || !isEmpty(b.getEpubLink())))
                p.getBooks().add(b);
        }


            final URL base = new URL(dir.getRefId());

        final Function<String, String> urler = path -> {
            try {
                final URL self = new URL(path);
                if (self.getProtocol().toLowerCase().startsWith("http"))
                    return self.toExternalForm();
            } catch (final Exception ignore) { }

            try {
                return new URL(base.getProtocol(), base.getHost(), base.getPort(), path).toExternalForm();
            } catch (MalformedURLException e) {
                logger.error(e.getMessage(), e);
            }

            return null;
        };
        return null;
    }

    public File loadFile(final TFile file, final String ext) {
        try {
            final File tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()), ext);
            try (final FileOutputStream bos = new FileOutputStream(tmp)) {
                getFile(file.getRefId(), bos);
            }

            return tmp;
        } catch (IOException e) {
            logger.error(file.getRefId() + " :: " + e.getMessage(), e);
        }

        return null;
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
            logger.debug("HTTP to: " + url);
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
            } else if (code / 300 == 1 && code % 300 < 100) {
                get(cn.getHeaderField("location"), os);
                return;
            } else
                logger.warn("Not succeded code: " + code);

            try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(128); final InputStream is = ((HttpURLConnection) cn).getErrorStream()) {
                int read;
                while ((read = is.read()) != -1)
                    bos.write(read);

                throw new Exception("CH response error message: " + new String(bos.toByteArray(), StandardCharsets.UTF_8));
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
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
