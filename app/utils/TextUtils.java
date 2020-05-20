package utils;

import play.Logger;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Denis Danilin | denis@danilin.name
 * 07.05.2018 11:41
 * single-auth ☭ sweat and blood
 */
public class TextUtils {
    private static final Logger.ALogger logger = Logger.of(TextUtils.class);
    public static final SecureRandom rnd;
    private static final Pattern
            singleQuotes = Pattern.compile("'?( |$)(?=(([^']*'){2})*[^']*$)'?"),
            doubleQuotes = Pattern.compile("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?"),
            winQuotes = Pattern.compile("«?( |$)(?=(«[^«»]*»)*[^»]*$)»?"),
            spaces = Pattern.compile("\\s+");

    static {
        rnd = new SecureRandom();
        rnd.setSeed(System.currentTimeMillis());
    }

    public static String capitalize(final String s) {
        return capitalize(s, true);
    }

    public static String capitalize(final String s, final boolean row) {
        if (s.length() < 2) return s.toUpperCase();

        return s.substring(0, 1).toUpperCase() + (row ? s.substring(1).toLowerCase() : s.substring(1));
    }

    public static String decapitalize(final String s) {
        return decapitalize(s, true);
    }

    public static String decapitalize(final String s, final boolean row) {
        if (s.length() < 2) return s.toUpperCase();

        return s.substring(0, 1).toLowerCase() + (row ? s.substring(1).toLowerCase() : s.substring(1));
    }


    public static long getLong(final Object value, final int radix) {
        try {
            return Long.parseLong(String.valueOf(value), radix);
        } catch (Exception ee) {
            return 0;
        }
    }

    public static long getLong(final Object value) {
        try {
            return Long.decode(String.valueOf(value));
        } catch (Exception e) {
            try {
                return Long.parseLong(value.toString().replaceAll("([^0-9-])", ""));
            } catch (Exception ee) {
                return 0;
            }
        }
    }


    public static String notNull(final Object o) {
        if (o == null)
            return "";

        return String.valueOf(o).trim();
    }

    public static String notNull(final Object o, final String defaultValue) {
        final String s = notNull(o);

        return s.isEmpty() ? defaultValue : s;
    }

    public static int getInt(final Object value) {
        if (value != null) {
            final String param = notNull(value);

            try {
                return Integer.decode(param);
            } catch (final Exception e) {
                try {
                    return Integer.parseInt(param.replaceAll("([^0-9-])", ""));
                } catch (final Exception ignore) { }
            }
        }

        return 0;
    }

    public static boolean isEmpty(final Object o) {
        if (o == null)
            return true;

        final Class<?> c = o.getClass();

        if (CharSequence.class.isAssignableFrom(c))
            return notNull(o).isEmpty();

        if (c.isArray())
            return Array.getLength(o) <= 0;

        if (Collection.class.isAssignableFrom(c))
            return ((Collection<?>) o).isEmpty();

        if (Map.class.isAssignableFrom(c))
            return ((Map<?, ?>) o).isEmpty();

        return notNull(o).isEmpty();
    }

    public static List<String> strings2paths(final String current, final String... parts) {
        return strings2paths(Paths.get(current), parts);
    }

    public static List<String> strings2paths(final Path current, final String... parts) {
        return Arrays.stream(parts)
                .filter(p -> !isEmpty(p))
                .map(s -> {
                    try {
                        final Path p = Paths.get(s);

                        return Paths.get((p.isAbsolute() ? p : current.resolve(p)).toFile().getCanonicalPath()).toString();
                    } catch (final Exception ignore) { }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static String validatePath(final String raw, final String current) {
        try {
            final Path p = Paths.get(raw);

            return Paths.get((p.isAbsolute() ? p : Paths.get(current).resolve(p)).toFile().getCanonicalPath()).toString();
        } catch (final Exception ignore) { }

        return null;
    }

    private static final Pattern md = Pattern.compile("([#\\.\\(\\)\\*\\[\\]\"`'~_-])");

    public static String escapeMd(final String s) {
        return md.matcher(s).replaceAll("\\\\$1");
    }

}
