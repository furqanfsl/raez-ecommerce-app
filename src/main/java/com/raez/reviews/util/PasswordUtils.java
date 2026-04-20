package com.raez.reviews.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtils {
    private PasswordUtils() {
    }

    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public static boolean matches(String plainText, String storedHash) {
        return matches(plainText, storedHash, null);
    }

    public static boolean matches(String plainText, String storedHash, String fallbackPassword) {
        if (plainText == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (storedHash.contains("placeholder")) {
            return fallbackPassword != null && fallbackPassword.equals(plainText);
        }
        if (looksLikeBcrypt(storedHash)) {
            try {
                return BCrypt.checkpw(plainText, storedHash)
                        || (fallbackPassword != null && fallbackPassword.equals(plainText));
            } catch (IllegalArgumentException exception) {
                return fallbackPassword != null && fallbackPassword.equals(plainText);
            }
        }
        return hash(plainText).equalsIgnoreCase(storedHash);
    }

    private static boolean looksLikeBcrypt(String storedHash) {
        return storedHash.startsWith("$2a$")
                || storedHash.startsWith("$2b$")
                || storedHash.startsWith("$2y$");
    }
}
