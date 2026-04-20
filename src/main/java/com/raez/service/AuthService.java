package com.raez.service;

import com.raez.db.DBConnection;
import com.raez.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Authenticates against {@code users} + {@code user_roles} + {@code roles} using SHA-256 hashes.
 */
public final class AuthService {

    private AuthService() {}

    public record AuthenticatedSession(User user, Set<String> allRoleNames) {}

    /**
     * Validates email + password hash against the database and returns the user plus all role names.
     */
    public static Optional<AuthenticatedSession> authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            return Optional.empty();
        }
        String trimmed = email.trim();
        String hashedPassword = DBConnection.hashPassword(password);
        String sql = """
            SELECT u.userID, u.firstName, u.lastName, u.email,
                   u.username, u.isActive, r.roleName
            FROM users u
            JOIN user_roles ur ON ur.userID = u.userID
            JOIN roles      r  ON r.roleID  = ur.roleID
            WHERE u.email = ? AND u.passwordHash = ? AND u.isActive = 1
            """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, hashedPassword);
            ResultSet rs = ps.executeQuery();
            Set<String> roleNames = new LinkedHashSet<>();
            Integer userId = null;
            String firstName = null;
            String lastName = null;
            String em = null;
            String username = null;
            int isActive = 1;
            while (rs.next()) {
                if (userId == null) {
                    userId = rs.getInt("userID");
                    firstName = rs.getString("firstName");
                    lastName = rs.getString("lastName");
                    em = rs.getString("email");
                    username = rs.getString("username");
                    isActive = rs.getInt("isActive");
                }
                roleNames.add(rs.getString("roleName"));
            }
            if (userId == null) {
                return Optional.empty();
            }
            String chosenRole = pickRoleForRouting(roleNames);
            User user = new User(userId, firstName, lastName, em, chosenRole, isActive, username, null);
            return Optional.of(new AuthenticatedSession(user, roleNames));
        } catch (SQLException e) {
            System.err.println("AuthService.authenticate: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Selects the highest-privilege role for routing.
     * Priority: super_admin &gt; product_admin &gt; customer_admin &gt; customer
     */
    public static String pickRoleForRouting(Set<String> roles) {
        for (String r : new String[]{"super_admin", "product_admin", "customer_admin", "warehouse_admin", "delivery_admin", "orders_admin", "orders_user", "finance_admin", "finance_user", "customer"}) {
            if (roles.contains(r)) return r;
        }
        return roles.isEmpty() ? "customer" : roles.iterator().next();
    }
}
