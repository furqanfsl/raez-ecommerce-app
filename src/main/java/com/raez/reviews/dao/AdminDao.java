package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.raez.reviews.model.AdminUser;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.PasswordUtils;

public class AdminDao {
    private final DatabaseManager databaseManager;

    public AdminDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<AdminUser> authenticate(String identifier, String plainTextPassword) {
        String sql = """
                SELECT u.userID, u.username, u.email, u.firstName, u.lastName, u.passwordHash, u.isActive, r.roleName
                FROM users u
                JOIN user_roles ur ON ur.userID = u.userID
                JOIN roles r ON r.roleID = ur.roleID
                WHERE (LOWER(u.username) = LOWER(?) OR LOWER(u.email) = LOWER(?))
                  AND r.roleName IN ('reviews_admin', 'super_admin')
                ORDER BY CASE WHEN r.roleName = 'reviews_admin' THEN 0 ELSE 1 END
                """;
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (!PasswordUtils.matches(plainTextPassword, resultSet.getString("passwordHash"),
                            fallbackPassword(resultSet.getString("roleName")))) {
                        continue;
                    }
                    return Optional.of(new AdminUser(
                            resultSet.getInt("userID"),
                            resultSet.getString("username"),
                            buildDisplayName(resultSet),
                            resultSet.getInt("isActive") == 1));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to authenticate admin user.", exception);
        }
    }

    private String buildDisplayName(ResultSet resultSet) throws SQLException {
        String firstName = resultSet.getString("firstName");
        String lastName = resultSet.getString("lastName");
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isBlank() ? resultSet.getString("username") : fullName;
    }

    private String fallbackPassword(String roleName) {
        return switch (roleName) {
            case "super_admin" -> "admin123";
            case "reviews_admin" -> "reviews123";
            default -> null;
        };
    }
}
