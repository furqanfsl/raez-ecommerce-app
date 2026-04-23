package com.raez.reviews.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    private static final DateTimeFormatter STORAGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private TimeUtils() {
    }

    public static String toStorage(LocalDateTime value) {
        return value.format(STORAGE_FORMATTER);
    }

    public static LocalDateTime fromStorage(String value) {
        try {
            return LocalDateTime.parse(value, STORAGE_FORMATTER);
        } catch (RuntimeException exception) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    public static String toDisplay(LocalDateTime value) {
        return value.format(DISPLAY_FORMATTER);
    }
}
