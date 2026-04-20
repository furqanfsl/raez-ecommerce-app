package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.raez.reviews.util.DatabaseManager;

public class OrderDao {
    private final DatabaseManager databaseManager;

    public OrderDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasPurchasedProduct(int customerId, int productId) {
        String sql = """
                SELECT COUNT(*)
                FROM orders
                JOIN order_items ON order_items.orderID = orders.orderID
                WHERE orders.customerID = ?
                  AND order_items.productID = ?
                  AND orders.status = 'Delivered'
                """;
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setInt(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to verify purchase history.", exception);
        }
    }
}
