package states;

import model.user.TgUser;
import services.BotApi;
import services.DataStore;
import utils.LangMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:56
 * tfs ☭ sweat and blood
 */
public class OpdsSearcher extends AState {
    private final String query;
    private boolean searched;
    private final Set<UUID> found;

    public OpdsSearcher(final UUID entryId, final String query) {
        this.entryId = entryId;
        this.query = query;
        searched = false;
        found = new HashSet<>();
    }

    public OpdsSearcher(final String encoded) {
        final int f = encoded.indexOf(':');
        final int s = encoded.indexOf(':', f+1);
        entryId = UUID.fromString(encoded.substring(0, f));
        found = s == f + 1 ? new HashSet<>() : Arrays.stream(encoded.substring(f + 1, s).split(",")).map(UUID::fromString).collect(Collectors.toSet());
        query = encoded.substring(s + 1);
        searched = true;
    }

    @Override
    public void display(final TgUser user, final BotApi api, final DataStore store) {
        if (!searched) {
            store.doOpdsSearch(query, user);
        }
    }

    @Override
    protected String encode() {
        return entryId + ":" + (!searched || isEmpty(found) ? "" : found.stream().map(UUID::toString).collect(Collectors.joining(","))) + ":" + query;
    }

    @Override
    public LangMap.Value helpValue(final TgUser user) {
        return LangMap.Value.SEARCHED_HELP;
    }
}
