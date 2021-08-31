package play.libs.ws.ahc;

import play.Logger;
import play.libs.ws.InMemoryBodyWritable;
import play.libs.ws.StandaloneWSResponse;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSRequestFilter;
import play.shaded.ahc.org.asynchttpclient.util.HttpUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.TextUtils.generateUuid;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.09.2020
 * core ☭ sweat and blood
 */
public class WsCurlLogger implements WSRequestFilter {
    private static final Logger.ALogger logger = Logger.of(WsCurlLogger.class);
    private static final Pattern SINGLE_QUOTE_REPLACE = Pattern.compile("'", Pattern.LITERAL);
    private static final Map<String, WsCurlLogger> reserva = new ConcurrentHashMap<>();

    public static WsCurlLogger continueOf(final String tracing) {
        if (tracing == null)
            return new WsCurlLogger();

        final WsCurlLogger logger = reserva.get(tracing);

        if (logger != null)
            return logger;

        return new WsCurlLogger(tracing);
    }

    public static WsCurlLogger lastOf(final String tracing) {
        if (tracing == null)
            return new WsCurlLogger();

        return reserva.containsKey(tracing) ? reserva.remove(tracing) : new WsCurlLogger(tracing);
    }

    private final String tracing;

    public WsCurlLogger() {
        tracing = generateUuid().toString();
    }

    public WsCurlLogger(final String tracing) {
        this.tracing = tracing == null ? generateUuid().toString() : tracing;

        reserva.put(tracing, this);
    }

    @Override
    public WSRequestExecutor apply(final WSRequestExecutor requestExecutor) {
        return request -> {
            if (logger.isDebugEnabled()) {
                request.addHeader("tracing", tracing);
                logger.debug(toCurl((StandaloneAhcWSRequest) request));
            }

            return requestExecutor
                    .apply(request)
                    .thenApply(response -> {
                        if (logger.isDebugEnabled())
                            logger.debug(toPlain(tracing, response));
                        return response;
                    })
                    .exceptionally(e -> {
                        if (logger.isDebugEnabled())
                            logger.error("ERROR 'tracing': " + quote(tracing) + (request.getRequestTimeout().isPresent() ? "with timeout: " + request.getRequestTimeout().get() + ";" +
                                    " " : "[no timeout]") + ": " + e.getMessage(), e);
                        else
                            logger.error(request.getUrl() + ": " + e.getMessage(), e);

                        return null;
                    });
        };
    }

    private String toPlain(final String tracing, final StandaloneWSResponse response) {
        final StringBuilder s = new StringBuilder(16);

        s.append("Trace: ").append(quote(tracing)).append('\n');
        s.append("Status: ").append(response.getStatus()).append(" [").append(notNull(response.getStatusText(), "no_text")).append("]\n");
        response.getHeaders().forEach((name, values) -> values.forEach(v -> s.append("H>> '").append(quote(name)).append(": ").append(quote(v)).append("'\\\n")));
        response.getCookies().forEach(cookie -> s.append("C>> '").append(cookie.getName()).append('=').append(cookie.getValue()).append("'\\\n"));
        s.append("B>> `").append(notNull(response.getBody(), "[No body in response]")).append("`");

        return s.toString();
    }

    private String toCurl(final StandaloneAhcWSRequest request) {
        final StringBuilder b = new StringBuilder("curl -v -X ").append(request.getMethod()).append(" '").append(quote(request.getUrl())).append("' ");
        request.getAuth().ifPresent(auth -> b.append("-H 'Authorization: Basic ").append(quote(Base64.getUrlEncoder().encodeToString((auth.getUsername() + ':' + auth.getPassword()).getBytes(StandardCharsets.US_ASCII)))).append('\'').append(" "));
        request.getHeaders().forEach((name, values) -> values.forEach(v -> b.append("-H '").append(quote(name)).append(": ").append(quote(v)).append('\'').append(" ")));
        request.getCookies().forEach(cookie -> b.append("-b '").append(cookie.getName()).append('=').append(cookie.getValue()).append('\'').append(" "));
        request.getBody().ifPresent(requestBody -> {
            if (!(requestBody instanceof InMemoryBodyWritable)) return;

            b.append(" -d '").append(quote(((InMemoryBodyWritable) requestBody).body().get().decodeString(findCharset(request)))).append('\'');
        });

        return b.toString();
    }

    private static String findCharset(StandaloneAhcWSRequest request) {
        return Optional.ofNullable(request.getContentType())
                .flatMap(contentType -> contentType.map(HttpUtils::extractContentTypeCharsetAttribute))
                .orElse(StandardCharsets.UTF_8).name();
    }

    private static String quote(String unsafe) {
        return SINGLE_QUOTE_REPLACE.matcher(unsafe).replaceAll(Matcher.quoteReplacement("'\\''"));
    }
}
