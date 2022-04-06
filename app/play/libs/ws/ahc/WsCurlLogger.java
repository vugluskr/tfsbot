package play.libs.ws.ahc;

import play.Logger;
import play.libs.ws.InMemoryBodyWritable;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSRequestFilter;
import play.shaded.ahc.org.asynchttpclient.util.HttpUtils;
import services.BotApi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.TextUtils.generateUuid;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.09.2020
 * core ☭ sweat and blood
 */
public class WsCurlLogger implements WSRequestFilter {
    private static final Logger.ALogger logger = Logger.of(BotApi.class);
    private static final Pattern SINGLE_QUOTE_REPLACE = Pattern.compile("'", Pattern.LITERAL);

    private final String tracing;

    public WsCurlLogger() {
        tracing = generateUuid().toString();
    }

    public WsCurlLogger(final String tracing) {
        this.tracing = tracing == null ? generateUuid().toString() : tracing;
    }

    @Override
    public WSRequestExecutor apply(final WSRequestExecutor requestExecutor) {
        return request -> requestExecutor
                .apply(request)
                .thenApply(response -> {
                    logger.debug(toCurl((StandaloneAhcWSRequest) request) + "\t=>\n" + response.getBody());
                    return response;
                })
                .exceptionally(e -> {
                    logger.debug(toCurl((StandaloneAhcWSRequest) request) + "\t=>\n" + e.getMessage(), e);

                    return null;
                });
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
