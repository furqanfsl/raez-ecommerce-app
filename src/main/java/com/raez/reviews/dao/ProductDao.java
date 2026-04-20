package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.raez.reviews.model.Product;
import com.raez.reviews.util.DatabaseManager;

public class ProductDao {
    private static final String PRODUCT_SELECT = """
            SELECT p.productID,
                   p.name,
                   COALESCE(c.categoryName, 'Uncategorised') AS category,
                   p.status,
                   COALESCE((
                       SELECT AVG(rr.rating)
                       FROM reviews_reviews rr
                       WHERE rr.productID = p.productID
                         AND rr.status = 'ACTIVE'
                   ), 0) AS averageRating,
                   (
                       SELECT COUNT(*)
                       FROM reviews_reviews rr
                       WHERE rr.productID = p.productID
                         AND rr.status = 'ACTIVE'
                   ) AS reviewCount
            FROM products p
            LEFT JOIN categories c ON c.categoryID = p.categoryID
            """;

    private final DatabaseManager databaseManager;

    public ProductDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Product> findAllActive() {
        return findActiveBySearch("");
    }

    public List<Product> findActiveBySearch(String searchText) {
        String sql = PRODUCT_SELECT
                + """
                WHERE p.status = 'active'
                  AND (LOWER(p.name) LIKE ? OR LOWER(COALESCE(c.categoryName, '')) LIKE ?)
                ORDER BY p.name
                """;
        String search = "%" + searchText.toLowerCase() + "%";
        List<Product> products = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, search);
            statement.setString(2, search);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapProduct(resultSet));
                }
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load products.", exception);
        }
    }

    public Optional<Product> findById(int productId) {
        try (Connection connection = databaseManager.getConnection()) {
            return findById(connection, productId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load product details.", exception);
        }
    }

    public Optional<Product> findById(Connection connection, int productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(PRODUCT_SELECT + " WHERE p.productID = ?")) {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProduct(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private Product mapProduct(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getInt("productID"),
                resultSet.getString("name"),
                resultSet.getString("category"),
                "active".equalsIgnoreCase(resultSet.getString("status")),
                resultSet.getDouble("averageRating"),
                resultSet.getInt("reviewCount"));
    }
}
