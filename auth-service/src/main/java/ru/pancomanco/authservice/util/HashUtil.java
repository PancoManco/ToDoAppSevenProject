package ru.pancomanco.authservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256Hex(String value) {
        byte[] hash = sha256(value);

        return HexFormat.of().formatHex(hash);
    }

    public static String sha256Base64Url(String value) {
        byte[] hash = sha256(value);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
    }

    private static byte[] sha256(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value to hash must not be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256 hash", ex);
        }
    }

    public static String sha256Base64UrlNormalizedEmail(String email) {
        String normalizedEmail = EmailUtil.normalize(email);

        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email to hash must not be null");
        }

        return sha256Base64Url(normalizedEmail);
    }
}
