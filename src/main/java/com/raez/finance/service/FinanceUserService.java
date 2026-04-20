package com.raez.finance.service;

import com.raez.finance.dao.FinanceUserDao;
import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.util.FinanceValidationUtils;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Business logic layer for creating and managing FinanceUser accounts.
 * Validates inputs before passing to FinanceUserDao.
 */
public class FinanceUserService {

    private final FinanceUserDao fUserDao = new FinanceUserDao();

    /**
     * Creates a new FinanceUser with a BCrypt-hashed password.
     * The lastLogin is set to NULL so the first login triggers the set-password flow.
     *
     * @param email         Must end with @raez.org.uk
     * @param username      Required, non-blank
     * @param plainPassword Required, non-blank (will be BCrypt-hashed)
     * @param role          ADMIN or FINANCE_USER
     * @param firstName     Optional
     * @param lastName      Optional
     * @param phone         Optional
     * @param active        true = account is active
     */
    public void createUser(
            String   email,
            String   username,
            String   plainPassword,
            FinanceUserRole role,
            String   firstName,
            String   lastName,
            String   phone,
            String   staffID,
            String   addressLine1,
            String   addressLine2,
            String   addressLine3,
            boolean  active) {

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (!FinanceValidationUtils.isRaezEmail(email))
            throw new IllegalArgumentException("Email must end with @raez.org.uk.");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Password is required.");
        if (role == null)
            throw new IllegalArgumentException("Role is required.");

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        try {
            fUserDao.insertUser(
                email.trim(),
                username.trim(),
                hash,
                role.name(),
                emptyToNull(firstName),
                emptyToNull(lastName),
                emptyToNull(phone),
                emptyToNull(staffID),
                emptyToNull(addressLine1),
                emptyToNull(addressLine2),
                emptyToNull(addressLine3),
                active
            );
        } catch (Exception e) {
            // Wrap with readable message — common case is a UNIQUE constraint violation
            String msg = e.getMessage() != null ? e.getMessage() : "Database error";
            if (msg.toLowerCase().contains("unique")) {
                throw new RuntimeException(
                    "A user with that email or username already exists.", e);
            }
            throw new RuntimeException("Failed to create user: " + msg, e);
        }
    }

    private static String emptyToNull(String settingValue) {
        if (settingValue == null) return null;
        String v = settingValue.trim();
        return v.isEmpty() ? null : v;
    }
}