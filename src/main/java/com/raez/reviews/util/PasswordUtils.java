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
        if (plainText == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (looksLikeBcrypt(storedHash)) {
            try {
                return BCrypt.checkpw(plainText, storedHash);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        // Legacy SHA-256 hex hash — kept for accounts that haven't been
        // migrated by util.MigratePasswords yet. New writes always go through
        // BCrypt.hashpw (see PasswordResetService, CustomerDAO.register, etc.).
        return hash(plainText).equalsIgnoreCase(storedHash);
    }

    private static boolean looksLikeBcrypt(String storedHash) {
        return storedHash.startsWith("$2a$")
                || storedHash.startsWith("$2b$")
                || storedHash.startsWith("$2y$");
    }
}
