package utils;

import java.security.MessageDigest;

public class Sha2Digest {

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
