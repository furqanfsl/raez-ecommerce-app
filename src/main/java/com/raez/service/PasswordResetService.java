package com.raez.service;

import com.raez.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles password recovery: generates a short recovery code, stores it in
 * password_reset_tokens, and sends it to the user via EmailService.
 */
public class PasswordResetService {

    private static final SecureRandom RNG = new SecureRandom();

    public enum Result { SENT, EMAIL_NOT_FOUND, SMTP_DISABLED, FAILED }

    /** Starts a recovery flow for the given email. Always silent-safe. */
    public static Result startRecovery(String email) {
        if (email == null || email.isBlank()) return Result.FAILED;

        Integer userId = findUserId(email.trim());
        if (userId == null) return Result.EMAIL_NOT_FOUND;

        String code = generateCode();
        String expiry = LocalDateTime.now().plusMinutes(30)
                          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (!storeToken(userId, code, expiry)) return Result.FAILED;

        String body =
            "Hello,\n\n" +
            "A password reset was requested for your RAEZ account.\n" +
            "Your recovery code: " + code + "\n\n" +
            "This code expires in 30 minutes.\n" +
            "If you did not request this, ignore this email.\n\n" +
            "— RAEZ Support";

        boolean sent = EmailService.send(email, "RAEZ Password Recovery Code", body);
        return sent ? Result.SENT : Result.SMTP_DISABLED;
    }

    private static Integer findUserId(String email) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT userID FROM users WHERE email = ? AND isActive = 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            System.err.println("PasswordResetService.findUserId failed: " + e.getMessage());
            return null;
        }
    }

    private static boolean storeToken(int userId, String code, String expiry) {
        String sql = "INSERT INTO password_reset_tokens (userID, token, expiryTime, isUsed) "
                   + "VALUES (?, ?, ?, 0)";
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setString(3, expiry);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PasswordResetService.storeToken failed: " + e.getMessage());
            return false;
        }
    }

    private static String generateCode() {
        int n = 100000 + RNG.nextInt(900000); // 6-digit
        return String.valueOf(n);
    }
}
