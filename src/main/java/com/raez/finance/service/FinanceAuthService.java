package com.raez.finance.service;

import com.raez.finance.dao.FinanceUserDao;
import com.raez.finance.dao.FinancePasswordResetTokenDao;
import com.raez.finance.model.FinanceUser;
import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.util.FinanceDatabaseConnection;
import com.raez.finance.util.FinancePasswordGenerator;
import com.raez.finance.util.FinanceValidationUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FinanceAuthService {
    private static boolean demoUsersChecked = false;

    // ── Checked exception: caller must switch to the set-password screen ──
    public static class FirstLoginRequiredException extends RuntimeException {
        public FirstLoginRequiredException(String message) {
            super(message);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LOGIN
    //  ─────────────────────────────────────────────────────────────
    //  BUG FIX (v2): The original code updated lastLogin to
    //  CURRENT_TIMESTAMP before throwing FirstLoginRequiredException.
    //  This meant completeFirstLogin() found lastLogin != NULL and
    //  threw "already completed first-time setup."
    //
    //  Fix: only update lastLogin for NORMAL logins.
    //  For first-time logins, lastLogin stays NULL in the DB so
    //  completeFirstLogin() can safely update it after the password
    //  has been set.
    // ══════════════════════════════════════════════════════════════

    public FinanceUser login(String usernameOrEmail, String plainPassword) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank())
            throw new IllegalArgumentException("Username or email is required.");
        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Password is required.");

        String lookup = usernameOrEmail.trim();

        try (Connection conn = FinanceDatabaseConnection.getConnection()) {
            ensureDemoUsers(conn);
            String sql = buildAuthSql(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, lookup);
                stmt.setString(2, lookup);
                ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new IllegalArgumentException("Invalid username/email or password.");

            String storedHash = rs.getString("passwordHash");

            // Guard against placeholder hashes from seed data
            if (storedHash == null || storedHash.isBlank()) {
                throw new IllegalArgumentException(
                    "This account has no password set. Contact your administrator.");
            }
            if (!storedHash.startsWith("$2")) {
                throw new IllegalArgumentException(
                    "This account's password needs to be reset. " +
                    "Run FinanceFixPlaceholderPasswordHashes or contact an administrator.");
            }

            if (!BCrypt.checkpw(plainPassword, storedHash)) {
                throw new IllegalArgumentException("Invalid username/email or password.");
            }

            int    id        = rs.getInt("userID");
            String email     = rs.getString("email");
            String username  = rs.getString("username");
            String roleStr   = rs.getString("role");
            String firstName = rs.getString("firstName");
            String lastName  = rs.getString("lastName");
            boolean isActive = rs.getInt("isActive") == 1;

            LocalDateTime lastLogin = parseLastLogin(rs.getString("lastLogin"));
            boolean firstLogin = (lastLogin == null);   // null lastLogin ⇒ first-time login

            FinanceUserRole role = parseRole(roleStr);

            FinanceUser user = new FinanceUser(id, email, username, storedHash,
                                   role, firstName, lastName, isActive, lastLogin);

            if (firstLogin) {
                // ── KEY FIX: do NOT update lastLogin here ──────────────
                // lastLogin stays NULL so completeFirstLogin() can detect
                // and update it after the new password has been saved.
                throw new FirstLoginRequiredException(
                    "First-time login detected — password change required.");
            }

            // Normal login: update lastLogin timestamp
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE users SET lastLogin = CURRENT_TIMESTAMP WHERE userID = ?")) {
                upd.setInt(1, id);
                upd.executeUpdate();
            } catch (Exception ignored) {
                // Non-fatal — do not prevent the user from logging in
            }

                FinanceSessionManager.startSession(user);
                return user;
            }

        } catch (FirstLoginRequiredException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  COMPLETE FIRST LOGIN
    //  Called from the set-password screen after the user has
    //  chosen a new password.
    //
    //  Because login() no longer sets lastLogin for first-time users,
    //  lastLogin is still NULL here, which is the correct pre-condition.
    //  We update BOTH passwordHash and lastLogin in one atomic UPDATE
    //  so the next login is treated as a normal login.
    // ══════════════════════════════════════════════════════════════

    public FinanceUser completeFirstLogin(String usernameOrEmail, String newPlainPassword) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank())
            throw new IllegalArgumentException("Username or email is required.");
        if (newPlainPassword == null || newPlainPassword.isBlank())
            throw new IllegalArgumentException("New password is required.");

        String lookup = usernameOrEmail.trim();

        try (Connection conn = FinanceDatabaseConnection.getConnection()) {
            String selectSql = buildAuthSql(conn);
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {

                stmt.setString(1, lookup);
                stmt.setString(2, lookup);
                ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new IllegalArgumentException(
                    "Account not found or inactive. Contact an administrator.");

            int    id        = rs.getInt("userID");
            String email     = rs.getString("email");
            String username  = rs.getString("username");
            String roleStr   = rs.getString("role");
            String firstName = rs.getString("firstName");
            String lastName  = rs.getString("lastName");
            boolean isActive = rs.getInt("isActive") == 1;

            if (!isActive)
                throw new IllegalArgumentException(
                    "This account is inactive. Contact an administrator.");

            FinanceUserRole role = parseRole(roleStr);
            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(12));

            // Update password AND set lastLogin in one statement.
            // After this, lastLogin != NULL → subsequent logins are treated normally.
            String updateSql =
                "UPDATE users SET passwordHash = ?, lastLogin = CURRENT_TIMESTAMP " +
                "WHERE userID = ?";
            try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                upd.setString(1, newHash);
                upd.setInt(2, id);
                int updated = upd.executeUpdate();
                if (updated == 0) {
                    throw new IllegalArgumentException(
                        "Failed to update password. Please try again or contact an administrator.");
                }
            }

                FinanceUser user = new FinanceUser(id, email, username, newHash,
                                       role, firstName, lastName, true,
                                       LocalDateTime.now());
                FinanceSessionManager.startSession(user);
                return user;
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to complete first-time login: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD
    //  Generates a temp password and clears lastLogin so the next
    //  login triggers the set-password flow again.
    //  In dev: prints temp password to console.
    //  In production: send via email.
    // ══════════════════════════════════════════════════════════════

    private final FinanceUserDao fUserDao = new FinanceUserDao();
    private final FinancePasswordResetTokenDao resetTokenDao = new FinancePasswordResetTokenDao();

    public void requestTemporaryPassword(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required.");
        try {
            FinanceUser user = fUserDao.findByEmail(email.trim());
            if (user == null || !user.isActive()) return; // silent — never reveal existence
            String tempPassword = FinancePasswordGenerator.generate();
            String hash = BCrypt.hashpw(tempPassword, BCrypt.gensalt(12));
            fUserDao.setTemporaryPasswordAndClearLastLogin(user.getId(), hash);
            // Dev mode: print to console. Production: send via SMTP.
            System.out.println("[ForgotPassword] Temporary password for "
                + email.trim() + " → " + tempPassword);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not process request: " + e.getMessage(), e);
        }
    }

    /**
     * Same as {@link #requestPasswordResetEmail(String, String)} with {@code sendToEmail} omitted:
     * the message is sent to the account's registered email when present.
     */
    public void requestPasswordResetEmail(String accountLookup) {
        requestPasswordResetEmail(accountLookup, null);
    }

    /**
     * Creates a reset token for the account resolved by {@code accountLookup} (email or username)
     * and sends it to {@code sendToEmail} when SMTP is configured. If {@code sendToEmail} is null or
     * blank, uses the user's stored email; if that is also blank, throws.
     * If no active user matches, returns without sending (no account enumeration).
     */
    public void requestPasswordResetEmail(String accountLookup, String sendToEmail) {
        if (accountLookup == null || accountLookup.isBlank())
            throw new IllegalArgumentException("Account email or username is required.");
        try {
            FinanceUser user = fUserDao.findByEmailOrUsername(accountLookup.trim());
            if (user == null || !user.isActive()) return;

            String to = sendToEmail != null ? sendToEmail.trim() : "";
            if (to.isEmpty()) {
                to = user.getEmail() != null ? user.getEmail().trim() : "";
            }
            if (to.isEmpty())
                throw new IllegalArgumentException("No email on file for this account. Contact an administrator.");
            if (!FinanceValidationUtils.isValidEmailFormat(to))
                throw new IllegalArgumentException("Enter a valid email address for delivery.");

            String token = resetTokenDao.createToken(user.getId());
            FinanceSettingsService gs = FinanceSettingsService.getInstance();
            if (gs.isSmtpEnabled() && gs.getSmtpHost() != null && !gs.getSmtpHost().isBlank()) {
                try {
                    FinanceMailService.sendPasswordResetEmail(to, token, user.getUsername());
                } catch (Exception ex) {
                    System.err.println("[PasswordReset] Mail error: " + ex.getMessage());
                    System.err.println("[PasswordReset] Token for account " + user.getUsername() + ": " + token);
                }
            } else {
                System.err.println("[PasswordReset] SMTP disabled or host empty. Token for account "
                        + user.getUsername() + ": " + token);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not process request: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RESET PASSWORD WITH TOKEN (admin-generated one-time token)
    // ══════════════════════════════════════════════════════════════

    /**
     * Resets password using a valid one-time token only (token binds to the user).
     */
    public void resetPasswordWithToken(String token, String newPlainPassword) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Reset token is required.");
        if (newPlainPassword == null || newPlainPassword.length() < 8)
            throw new IllegalArgumentException("New password must be at least 8 characters.");

        try {
            int userId = resetTokenDao.findUserIdByValidToken(token.trim());
            if (userId <= 0)
                throw new IllegalArgumentException(
                    "Invalid or expired token. Request a new one or use Forgot password to get a new email.");

            FinanceUser user = fUserDao.findById(userId);
            if (user == null)
                throw new IllegalArgumentException("FinanceUser not found.");

            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(12));
            fUserDao.updatePasswordByUserId(userId, newHash);
            resetTokenDao.markUsed(token.trim());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Password reset failed: " + e.getMessage(), e);
        }
    }

    /** Legacy signature; {@code email} is ignored (token identifies the account). */
    public void resetPasswordWithToken(String email, String token, String newPlainPassword) {
        resetPasswordWithToken(token, newPlainPassword);
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    private static LocalDateTime parseLastLogin(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try { return LocalDateTime.parse(raw); }
            catch (DateTimeParseException ignored) { return null; }
        }
    }

    private static String buildAuthSql(Connection conn) throws SQLException {
        if (hasUsersRoleIdColumn(conn)) {
            return "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                    "       r.roleName AS role, u.firstName, u.lastName, u.isActive, u.lastLogin " +
                    "FROM users u " +
                    "JOIN roles r ON u.roleID = r.roleID " +
                    "WHERE (u.email = ? OR u.username = ?) AND u.isActive = 1 " +
                    "AND r.roleName IN ('finance_user','finance_admin','super_admin')";
        }
        return "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                "       r.roleName AS role, u.firstName, u.lastName, u.isActive, u.lastLogin " +
                "FROM users u " +
                "JOIN user_roles ur ON ur.userID = u.userID " +
                "JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE (u.email = ? OR u.username = ?) AND u.isActive = 1 " +
                "AND r.roleName IN ('finance_user','finance_admin','super_admin')";
    }

    private static boolean hasUsersRoleIdColumn(Connection conn) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "users", "roleID")) {
            return rs.next();
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static synchronized void ensureDemoUsers(Connection conn) {
        if (demoUsersChecked) return;
        try {
            int financeRoleId = findRoleId(conn, "finance_user");
            int adminRoleId = findRoleId(conn, "super_admin");
            if (adminRoleId <= 0) adminRoleId = findRoleId(conn, "finance_admin");
            if (financeRoleId <= 0 || adminRoleId <= 0) {
                demoUsersChecked = true;
                return;
            }
            upsertUserWithRole(conn, "admin@raez.com", "admin", "System", "Admin", adminRoleId);
            upsertUserWithRole(conn, "finance@raez.com", "finance", "Finance", "User", financeRoleId);
        } catch (Exception ignored) {
            // Never block login if demo-upsert fails.
        } finally {
            demoUsersChecked = true;
        }
    }

    private static int findRoleId(Connection conn, String roleName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT roleID FROM roles WHERE roleName = ?")) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("roleID") : -1;
            }
        }
    }

    private static void upsertUserWithRole(Connection conn, String email, String username,
                                           String firstName, String lastName, int roleId) throws SQLException {
        int userId = -1;
        try (PreparedStatement sel = conn.prepareStatement("SELECT userID FROM users WHERE email = ?")) {
            sel.setString(1, email);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) userId = rs.getInt("userID");
            }
        }
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(10));
        if (userId <= 0) {
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO users (email, username, passwordHash, firstName, lastName, isActive) VALUES (?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, email);
                ins.setString(2, username);
                ins.setString(3, hash);
                ins.setString(4, firstName);
                ins.setString(5, lastName);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) userId = keys.getInt(1);
                }
            }
        }
        if (userId > 0) {
            try (PreparedStatement ur = conn.prepareStatement(
                    "INSERT OR IGNORE INTO user_roles (userID, roleID) VALUES (?, ?)")) {
                ur.setInt(1, userId);
                ur.setInt(2, roleId);
                ur.executeUpdate();
            }
        }
    }

    private static FinanceUserRole parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank())
            throw new IllegalArgumentException("FinanceUser role is missing in database.");
        String normalized = roleStr.trim().toUpperCase().replace(" ", "_");
        // Backwards compatibility: older seed data used `role='USER'`
        if ("USER".equals(normalized) || "FINANCE_USER".equals(normalized)) return FinanceUserRole.FINANCE_USER;
        if ("FINANCE_ADMIN".equals(normalized) || "SUPER_ADMIN".equals(normalized)) return FinanceUserRole.ADMIN;
        try {
            return FinanceUserRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unsupported role in database: '" + roleStr + "'. " +
                "Allowed: ADMIN, FINANCE_USER (legacy: USER)");
        }
    }
}