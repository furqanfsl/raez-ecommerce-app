package com.raez.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies a plaintext password against a stored hash, transparently handling
 * both formats present in the codebase:
 *
 *   - BCrypt ($2a$ / $2b$ / $2y$): the target format produced by
 *     {@link com.raez.util.MigratePasswords} and customer signup.
 *   - SHA-256 (64 lowercase hex chars): the legacy format shipped in
 *     raez_seed_data.sql for demo accounts. Lets the installed-app demo
 *     login work on a fresh DB before MigratePasswords has been run.
 *
 * Anything else (null/blank/unknown) is treated as a verification failure.
 */
public final class PasswordVerifier {

    private PasswordVerifier() {}

    public static boolean verify(String plain, String storedHash) {
        if (plain == null || storedHash == null || storedHash.isBlank()) return false;
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(plain, storedHash);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (storedHash.length() == 64 && storedHash.chars().allMatch(PasswordVerifier::isHex)) {
            return sha256Hex(plain).equalsIgnoreCase(storedHash);
        }
        return false;
    }

    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String sha256Hex(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
