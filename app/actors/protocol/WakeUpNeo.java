package actors.protocol;

import model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public class WakeUpNeo {
    public final User user;
    public Map<String, Object> params;

    public WakeUpNeo(final User user) {
        this.user = user;
    }

    public WakeUpNeo with(final String name, final Object value) {
        if (name == null || value == null)
            return this;

        if (params == null)
            params = new HashMap<>(0);
        params.put(name, value);

        return this;
    }

    public int getInt(final String paramName, int... defaults) {
        if (params != null && params.containsKey(paramName))
            try {
                return (Integer) params.get(paramName);
            } catch (final Exception ignore) { }

        return defaults == null || defaults.length <= 0 ? 0 : defaults[0];
    }

    public long getLong(final String paramName, long... defaults) {
        if (params != null && params.containsKey(paramName))
            try {
                return (Long) params.get(paramName);
            } catch (final Exception ignore) { }

        return defaults == null || defaults.length <= 0 ? 0 : defaults[0];
    }
}
