package com.raez.reviews.util;

import java.util.regex.Pattern;

import com.raez.reviews.exception.BusinessException;

public final class ValidationUtils {
    public static final int MAX_COMMENT_LENGTH = 500;
    private static final Pattern DISALLOWED_PATTERN = Pattern.compile("[@,.#/\\\\?\"'~`$]");

    private ValidationUtils() {
    }

    public static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be a whole number between 1 and 5.");
        }
    }

    public static void validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("Review text is required.");
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException("Review text cannot be longer than 500 characters.");
        }
        if (DISALLOWED_PATTERN.matcher(comment).find()) {
            throw new BusinessException("Review text contains blocked special characters.");
        }
    }
}
