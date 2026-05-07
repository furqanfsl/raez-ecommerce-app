package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewAggregate;
import com.raez.reviews.model.ReviewSortOption;
import com.raez.reviews.model.ReviewStatus;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.TimeUtils;

public class ReviewDao {
    private static final String BASE_SELECT = """
            SELECT r.reviewID, r.productID, r.customerID, p.name AS productName,
                   c.name AS customerName, r.rating, r.comment, r.createdAt,
                   r.updatedAt, r.status, r.helpfulCount, r.unhelpfulCount
            FROM reviews_reviews r
            JOIN products p ON p.productID = r.productID
            JOIN customers c ON c.customerID = r.customerID
            """;
    private static final String INSERT_REVIEW_SQL = """
            INSERT INTO reviews_reviews(productID, customerID, rating, comment, createdAt, updatedAt, status, helpfulCount, unhelpfulCount)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 0, 0)
            """;
    private static final String UPDATE_CUSTOMER_REVIEW_SQL =
            "UPDATE reviews_reviews SET rating = ?, comment = ?, updatedAt = ? WHERE reviewID = ?";
    private static final String UPDATE_ADMIN_REVIEW_SQL =
            "UPDATE reviews_reviews SET rating = ?, comment = ?, status = ?, updatedAt = ? WHERE reviewID = ?";
    private static final String UPDATE_STATUS_SQL =
            "UPDATE reviews_reviews SET status = ?, updatedAt = ? WHERE reviewID = ?";
    private static final String CALCULATE_AGGREGATE_SQL = """
            SELECT COALESCE(AVG(rating), 0) AS averageRating, COUNT(*) AS reviewCount
            FROM reviews_reviews
            WHERE productID = ?
              AND status = 'ACTIVE'
            """;
    private static final String CALCULATE_DISTRIBUTION_SQL = """
            SELECT rating, COUNT(*) AS voteCount
            FROM reviews_reviews
            WHERE productID = ?
              AND status = 'ACTIVE'
            GROUP BY rating
            """;

    private final DatabaseManager databaseManager;

    public ReviewDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Review> findById(int reviewId) {
        try (Connection connection = databaseManager.getConnection()) {
            return findById(connection, reviewId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load the selected review.", exception);
        }
    }

    public Optional<Review> findById(Connection connection, int reviewId) throws SQLException {
        return findOptional(connection, BASE_SELECT + " WHERE r.reviewID = ?", statement -> statement.setInt(1, reviewId));
    }

    public Optional<Review> findByCustomerAndProduct(int customerId, int productId) {
        String sql = BASE_SELECT + " WHERE r.customerID = ? AND r.productID = ?";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setInt(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapReview(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to check the customer's existing review.", exception);
        }
    }

    public boolean existsByCustomerAndProduct(int customerId, int productId) {
        String sql = "SELECT COUNT(*) FROM reviews_reviews WHERE customerID = ? AND productID = ?";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setInt(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to verify duplicate review state.", exception);
        }
    }

    public int insert(Connection connection, int productId, int customerId, int rating, String comment, String createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_REVIEW_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, productId);
            statement.setInt(2, customerId);
            statement.setInt(3, rating);
            statement.setString(4, comment);
            statement.setString(5, createdAt);
            statement.setString(6, createdAt);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new SQLException("The review identifier was not returned.");
            }
        }
    }

    public void updateCustomerReview(Connection connection, int reviewId, int rating, String comment, String updatedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CUSTOMER_REVIEW_SQL)) {
            statement.setInt(1, rating);
            statement.setString(2, comment);
            statement.setString(3, updatedAt);
            statement.setInt(4, reviewId);
            statement.executeUpdate();
        }
    }

    public void updateAdminReview(Connection connection, int reviewId, int rating, String comment, ReviewStatus status, String updatedAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_ADMIN_REVIEW_SQL)) {
            statement.setInt(1, rating);
            statement.setString(2, comment);
            statement.setString(3, status.name());
            statement.setString(4, updatedAt);
            statement.setInt(5, reviewId);
            statement.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, int reviewId, ReviewStatus status, String updatedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setString(2, updatedAt);
            statement.setInt(3, reviewId);
            statement.executeUpdate();
        }
    }

    public void incrementVoteCount(Connection connection, int reviewId, boolean helpful) throws SQLException {
        String column = helpful ? "helpfulCount" : "unhelpfulCount";
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reviews_reviews SET " + column + " = " + column + " + 1 WHERE reviewID = ?")) {
            statement.setInt(1, reviewId);
            statement.executeUpdate();
        }
    }

    public ReviewAggregate calculateAggregate(Connection connection, int productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CALCULATE_AGGREGATE_SQL)) {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ReviewAggregate(resultSet.getDouble("averageRating"), resultSet.getInt("reviewCount"));
            }
        }
    }

    public int[] calculateDistribution(int productId) {
        // One slot per rating value keeps the chart logic simple and predictable.
        int[] ratingBuckets = new int[5];
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(CALCULATE_DISTRIBUTION_SQL)) {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int rating = resultSet.getInt("rating");
                    if (rating >= 1 && rating <= ratingBuckets.length) {
                        ratingBuckets[rating - 1] = resultSet.getInt("voteCount");
                    }
                }
            }
            return ratingBuckets;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load rating distribution data.", exception);
        }
    }

    public List<Review> findForProduct(int productId, ReviewSortOption sortOption) {
        String sql = BASE_SELECT
                + " WHERE r.productID = ? AND r.status IN ('ACTIVE', 'FLAGGED') ORDER BY "
                + sortClause(sortOption);
        return loadReviewList(sql, statement -> statement.setInt(1, productId),
                "Unable to load reviews for the chosen product.");
    }

    public List<Review> findByCustomer(int customerId) {
        String sql = BASE_SELECT
                + " WHERE r.customerID = ? AND r.status IN ('ACTIVE', 'FLAGGED')"
                + " ORDER BY r.updatedAt DESC";
        return loadReviewList(sql, statement -> statement.setInt(1, customerId),
                "Unable to load reviews for the current customer.");
    }

    public List<Review> findForAdmin(Integer productId, ReviewStatus status, String searchText) {
        AdminQuery adminQuery = buildAdminQuery(productId, status, searchText);
        return loadReviewList(adminQuery.sql(), statement -> bindParameters(statement, adminQuery.parameters()),
                "Unable to load moderation review list.");
    }

    private Optional<Review> findOptional(Connection connection, String sql, StatementConfigurer configurer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapReview(resultSet)) : Optional.empty();
            }
        }
    }

    private List<Review> loadReviewList(String sql, StatementConfigurer configurer, String errorMessage) {
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readReviewList(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private List<Review> readReviewList(ResultSet resultSet) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        while (resultSet.next()) {
            reviews.add(mapReview(resultSet));
        }
        return reviews;
    }

    private AdminQuery buildAdminQuery(Integer productId, ReviewStatus status, String searchText) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();
        if (productId != null) {
            sql.append(" AND r.productID = ? ");
            parameters.add(productId);
        }
        if (status != null) {
            sql.append(" AND r.status = ? ");
            parameters.add(status.name());
        }
        if (searchText != null && !searchText.isBlank()) {
            // This query only expands with fixed SQL fragments; user input is still bound as parameters.
            sql.append(" AND (LOWER(r.comment) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(p.name) LIKE ?) ");
            String likeValue = "%" + searchText.toLowerCase() + "%";
            parameters.add(likeValue);
            parameters.add(likeValue);
            parameters.add(likeValue);
        }
        sql.append(" ORDER BY r.updatedAt DESC ");
        return new AdminQuery(sql.toString(), parameters);
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private String sortClause(ReviewSortOption sortOption) {
        // Sorting uses enum-backed options so the ORDER BY clause stays on a safe whitelist.
        return switch (sortOption) {
            case RATING_HIGH -> "r.rating DESC, r.createdAt DESC";
            case HELPFULNESS -> "(r.helpfulCount - r.unhelpfulCount) DESC, r.createdAt DESC";
            case NEWEST -> "r.createdAt DESC";
        };
    }

    private Review mapReview(ResultSet resultSet) throws SQLException {
        return new Review(
                resultSet.getInt("reviewID"),
                resultSet.getInt("productID"),
                resultSet.getInt("customerID"),
                resultSet.getString("productName"),
                resultSet.getString("customerName"),
                resultSet.getInt("rating"),
                resultSet.getString("comment"),
                TimeUtils.fromStorage(resultSet.getString("createdAt")),
                TimeUtils.fromStorage(resultSet.getString("updatedAt")),
                ReviewStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("helpfulCount"),
                resultSet.getInt("unhelpfulCount"));
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }

    private record AdminQuery(String sql, List<Object> parameters) {
    }
}
