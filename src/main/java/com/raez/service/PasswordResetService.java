package com.raez.service;

import com.raez.db.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles password recovery: generates a short recovery code, stores it in
 * password_reset_tokens, and sends it to the user via EmailService.
 */
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);


    private static final SecureRandom RNG = new SecureRandom();

    public enum Result { SENT, EMAIL_NOT_FOUND, SMTP_DISABLED, FAILED }
    public enum ResetResult { SUCCESS, INVALID_CODE, EXPIRED, ALREADY_USED, FAILED }

    /** Starts a recovery flow for the given email. Always silent-safe. */
    public static Result startRecovery(String email) {
        if (email == null || email.isBlank()) return Result.FAILED;

        Integer userId = findUserId(email.trim());
        if (userId == null) return Result.EMAIL_NOT_FOUND;

        String code = generateCode();
        String expiry = LocalDateTime.now().plusMinutes(7)
                          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (!storeToken(userId, code, expiry)) return Result.FAILED;

        String body =
            "Hello,\n\n" +
            "A password reset was requested for your RAEZ account.\n" +
            "Your recovery code: " + code + "\n\n" +
            "This code expires in 7 minutes.\n" +
            "If you did not request this, ignore this email.\n\n" +
            "— RAEZ Support";

        boolean sent = EmailService.send(email, "RAEZ Password Recovery Code", body);
        return sent ? Result.SENT : Result.SMTP_DISABLED;
    }

    /**
     * Returns the most-recent unused recovery code for this email.
     * Only called when SMTP is disabled so the dialog can display it directly.
     */
    public static String getLatestPendingCode(String email) {
        String sql =
            "SELECT t.token FROM password_reset_tokens t " +
            "JOIN users u ON u.userID = t.userID " +
            "WHERE u.email = ? AND t.isUsed = 0 " +
            "ORDER BY t.tokenID DESC LIMIT 1";
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("token") : null;
            }
        } catch (Exception e) {
            return null;
        }
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
            log.error("{}", "PasswordResetService.findUserId failed: " + e.getMessage());
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
            log.error("{}", "PasswordResetService.storeToken failed: " + e.getMessage());
            return false;
        }
    }

    /** Verifies the recovery code for the email and, if valid, updates the password. */
    public static ResetResult verifyAndReset(String email, String code, String newPassword) {
        if (email == null || code == null || newPassword == null) return ResetResult.FAILED;
        email = email.trim(); code = code.trim();

        String sql =
            "SELECT t.tokenID, t.isUsed, t.expiryTime " +
            "FROM password_reset_tokens t " +
            "JOIN users u ON u.userID = t.userID " +
            "WHERE u.email = ? AND t.token = ? " +
            "ORDER BY t.tokenID DESC LIMIT 1";

        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return ResetResult.INVALID_CODE;
                if (rs.getInt("isUsed") == 1) return ResetResult.ALREADY_USED;
                LocalDateTime expiry = LocalDateTime.parse(rs.getString("expiryTime"));
                if (LocalDateTime.now().isAfter(expiry)) return ResetResult.EXPIRED;
                int tokenId = rs.getInt("tokenID");

                String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));

                // Update password
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE users SET passwordHash = ? WHERE email = ?")) {
                    upd.setString(1, hashed);
                    upd.setString(2, email);
                    upd.executeUpdate();
                }
                // Mark token used
                try (PreparedStatement mark = c.prepareStatement(
                        "UPDATE password_reset_tokens SET isUsed = 1 WHERE tokenID = ?")) {
                    mark.setInt(1, tokenId);
                    mark.executeUpdate();
                }
                return ResetResult.SUCCESS;
            }
        } catch (SQLException e) {
            log.error("{}", "PasswordResetService.verifyAndReset failed: " + e.getMessage());
            return ResetResult.FAILED;
        }
    }

    private static String generateCode() {
        int n = 100000 + RNG.nextInt(900000); // 6-digit
        return String.valueOf(n);
    }
}
