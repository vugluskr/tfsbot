package utils;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Denis Danilin | denis@danilin.name
 * 07.05.2018 11:41
 * single-auth ☭ sweat and blood
 */
public class TextUtils {
    public static final SecureRandom rnd;

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

    private static long tol(final byte[] buf, final int shift) {
        return (toi(buf, shift) << 32) + ((toi(buf, shift + 4) << 32) >>> 32);
    }

    private static long toi(final byte[] buf, int shift) {
        return (buf[shift] << 24)
                + ((buf[++shift] & 0xFF) << 16)
                + ((buf[++shift] & 0xFF) << 8)
                + (buf[++shift] & 0xFF);
    }

    public static UUID generateUuid() {
        final byte[] buffer = new byte[16];
        rnd.nextBytes(buffer);

        return generateUuid(buffer);
    }

    private static UUID generateUuid(final byte[] buffer) {
        long r1, r2;

        r1 = tol(buffer, 0);
        r2 = tol(buffer, 1);

        r1 &= ~0xF000L;
        r1 |= 4 << 12;
        r2 = ((r2 << 2) >>> 2);
        r2 |= (2L << 62);

        return new UUID(r1, r2);
    }

    private static final Pattern md = Pattern.compile("([!=#\\.\\(\\)\\*\\[\\]\"`'~_-])");

    public static String escapeMd(final String s) {
        return md.matcher(s).replaceAll("\\\\$1");
    }

    public static String mdBold(final Object o) {
        return wrap(o, '*');
    }

    public static String mdBoldU(final Object o) {
        return wrapU(o, '*');
    }

    public static String mdItalic(final Object o) {
        return wrap(o, '_');
    }

    public static String mdItalicU(final Object o) {
        return wrapU(o, '_');
    }

    private static String wrapU(final Object v, final char with) {
        return with + String.valueOf(v) + with;
    }

    private static String wrap(final Object v, final char with) {
        return with + escapeMd(String.valueOf(v)) + with;
    }

    public static String hash256(String data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data.getBytes());
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        final StringBuilder result = new StringBuilder();

        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));

        return result.toString();
    }
}
