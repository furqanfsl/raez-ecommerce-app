package Ecommerce1.Ecommerce1.util;
import java.util.regex.Pattern;
import java.util.Set;
/**
 * ValidationUtil — reusable validation logic used across all controllers.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles validation
 *  - Static utility methods: no need to instantiate
 *  - DRY: removes duplicated validation code from every controller
 */
public class Warehouse_ValidationUtil {
    private static final Set<String> VALID_USER_DOMAINS =
            Set.of("gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "icloud.com");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9.]+$");
    // Prevent instantiation
    private Warehouse_ValidationUtil() {}
    /**
     * Returns empty string if null, otherwise trims whitespace.
     */
    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }
    /**
     * Parses a non-negative integer from a string.
     * Returns -1 if invalid (caller should check for -1 and show error).
     */
    public static int parseNonNegative(String s) {
        try {
            int n = Integer.parseInt(s.trim());
            if (n < 0) return -1;
            return n;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    /**
     * Parses a positive integer (must be > 0) from a string.
     * Returns -1 if invalid.
     */
    public static int parsePositive(String s) {
        int n = parseNonNegative(s);
        return n <= 0 ? -1 : n;
    }
    /**
     * Validates a staff email — must contain raez.org.uk anywhere in the domain.
     * CHANGED: now accepts admin@raez.org.uk.wh and similar variants
     */
    public static boolean isValidStaffEmail(String email) {
        return email != null && email.toLowerCase().contains("raez.org.uk");
    }
    /**
     * Validates a user email — accepts raez.org.uk staff emails
     * OR standard public domains (gmail, hotmail, outlook, yahoo, icloud).
     * CHANGED: now accepts raez.org.uk emails for warehouse users too
     */
    public static ValidationResult validateUserEmail(String email) {
        if (email == null || !email.contains("@")) {
            return ValidationResult.fail("Please enter a valid email address.");
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return ValidationResult.fail("Please enter a valid email address.");
        }
        String username = parts[0];
        String domain   = parts[1].toLowerCase();
        // Accept raez.org.uk staff emails OR standard public domains
        if (domain.contains("raez.org.uk")) {
            return ValidationResult.ok();
        }
        if (!VALID_USER_DOMAINS.contains(domain)) {
            return ValidationResult.fail(
                "Only raez.org.uk, Gmail, Hotmail, Outlook, Yahoo, and iCloud email addresses are accepted.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.fail(
                "Email username can only contain letters, numbers, and periods.");
        }
        if (username.startsWith(".") || username.endsWith(".")) {
            return ValidationResult.fail(
                "Email username cannot start or end with a period.");
        }
        return ValidationResult.ok();
    }
    /**
     * Simple result object for validation checks.
     *
     * OOP principles applied:
     *  - Encapsulation: success/message are private
     *  - Factory methods: ok() and fail() instead of public constructor
     */
    public static class ValidationResult {
        private final boolean success;
        private final String  message;
        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
    }
}