package com.raez.util;

import java.util.regex.Pattern;

/**
 * Boundary input validators. Throw {@link IllegalArgumentException} with a
 * user-readable message — controllers catch and surface in an Alert/dialog.
 */
public final class Validators {

    private static final Pattern EMAIL_RE =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private Validators() {}

    public static String email(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String trimmed = s.trim();
        if (trimmed.length() > 254 || !EMAIL_RE.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        return trimmed;
    }

    public static String nonEmpty(String s, int maxLen, String fieldName) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String trimmed = s.trim();
        if (trimmed.length() > maxLen) {
            throw new IllegalArgumentException(
                fieldName + " must be at most " + maxLen + " characters.");
        }
        return trimmed;
    }

    public static double positive(double value, String fieldName) {
        if (Double.isNaN(value) || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
        return value;
    }

    public static int positiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
        return value;
    }
}
