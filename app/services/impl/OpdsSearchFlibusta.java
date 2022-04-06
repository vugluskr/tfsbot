package services.impl;

import com.sun.org.apache.xerces.internal.util.XMLChar;
import model.TFile;
import model.opds.Book;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.Logger;
import services.OpdsSearch;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
public class OpdsSearchFlibusta implements OpdsSearch {
    private static final Logger.ALogger logger = Logger.of(OpdsSearch.class);
    private static final String searchUrl = "https://flibusta.is/opds/search?searchType=books&searchTerm=";

    @Override
    public CompletionStage<List<Book>> search(final String query) {
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
