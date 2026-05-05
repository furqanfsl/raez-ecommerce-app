package com.raez.service;

import com.raez.db.DBConnection;
import com.raez.model.User;
import com.raez.util.PasswordVerifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticates against {@code users} + {@code user_roles} + {@code roles} using BCrypt.
 */
public final class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    private AuthService() {}

    public record AuthenticatedSession(User user, Set<String> allRoleNames) {}

    /**
     * Looks up the user by email, then verifies the supplied plaintext against
     * the stored hash via {@link PasswordVerifier#verify(String, String)}
     * (BCrypt for migrated rows, SHA-256 fallback for legacy seed rows).
     */
    public static Optional<AuthenticatedSession> authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            return Optional.empty();
        }
        String trimmed = email.trim();
        String sql = """
            SELECT u.userID, u.firstName, u.lastName, u.email,
                   u.username, u.passwordHash, u.isActive, r.roleName
            FROM users u
            JOIN user_roles ur ON ur.userID = u.userID
            JOIN roles      r  ON r.roleID  = ur.roleID
            WHERE u.email = ? AND u.isActive = 1
            """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ResultSet rs = ps.executeQuery();
            Set<String> roleNames = new LinkedHashSet<>();
            Integer userId = null;
            String firstName = null;
            String lastName = null;
            String em = null;
            String username = null;
            String storedHash = null;
            int isActive = 1;
            while (rs.next()) {
                if (userId == null) {
                    userId = rs.getInt("userID");
                    firstName = rs.getString("firstName");
                    lastName = rs.getString("lastName");
                    em = rs.getString("email");
                    username = rs.getString("username");
                    storedHash = rs.getString("passwordHash");
                    isActive = rs.getInt("isActive");
                }
                roleNames.add(rs.getString("roleName"));
            }
            if (userId == null || storedHash == null || storedHash.isBlank()) {
                return Optional.empty();
            }
            if (!PasswordVerifier.verify(password, storedHash)) {
                return Optional.empty();
            }
            String chosenRole = pickRoleForRouting(roleNames);
            User user = new User(userId, firstName, lastName, em, chosenRole, isActive, username, null);
            return Optional.of(new AuthenticatedSession(user, roleNames));
        } catch (SQLException e) {
            log.error("{}", "AuthService.authenticate: " + e.getMessage());
            log.error("Error", e);
            return Optional.empty();
        }
    }

    /**
     * Selects the highest-privilege role for routing.
     * Priority: super_admin &gt; product_admin &gt; customer_admin &gt; customer
     */
    public static String pickRoleForRouting(Set<String> roles) {
        for (String r : new String[]{"super_admin", "product_admin", "customer_admin", "warehouse_admin", "delivery_admin", "orders_admin", "orders_user", "finance_admin", "finance_user", "reviews_admin", "customer"}) {
            if (roles.contains(r)) return r;
        }
        return roles.isEmpty() ? "customer" : roles.iterator().next();
    }
}
