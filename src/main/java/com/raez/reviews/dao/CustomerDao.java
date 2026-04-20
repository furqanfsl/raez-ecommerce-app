package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.raez.reviews.model.Customer;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.PasswordUtils;

public class CustomerDao {
    private final DatabaseManager databaseManager;

    public CustomerDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Customer> authenticate(String email, String plainTextPassword) {
        String sql = """
                SELECT c.customerID, c.name, c.email, c.status, u.passwordHash, u.isActive
                FROM customers c
                JOIN users u ON u.userID = c.userID
                WHERE LOWER(c.email) = LOWER(?)
                  AND EXISTS (
                      SELECT 1
                      FROM user_roles ur
                      JOIN roles r ON r.roleID = ur.roleID
                      WHERE ur.userID = u.userID
                        AND r.roleName = 'customer'
                  )
                """;
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (PasswordUtils.matches(plainTextPassword, resultSet.getString("passwordHash"))) {
                        return Optional.of(new Customer(
                                resultSet.getInt("customerID"),
                                resultSet.getString("name"),
                                resultSet.getString("email"),
                                resultSet.getInt("isActive") == 1
                                        && "active".equalsIgnoreCase(resultSet.getString("status"))));
                    }
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to authenticate customer.", exception);
        }
    }
}
