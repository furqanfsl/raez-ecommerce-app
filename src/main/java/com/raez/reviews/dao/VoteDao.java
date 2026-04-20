package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.raez.reviews.model.VoteType;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.TimeUtils;

public class VoteDao {
    private final DatabaseManager databaseManager;

    public VoteDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasExistingVote(int reviewId, int customerId) {
        String sql = "SELECT COUNT(*) FROM reviews_votes WHERE reviewID = ? AND customerID = ?";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reviewId);
            statement.setInt(2, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to verify vote status.", exception);
        }
    }

    public void insert(Connection connection, int reviewId, int customerId, VoteType voteType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO reviews_votes(reviewID, customerID, voteType, votedAt) VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, reviewId);
            statement.setInt(2, customerId);
            statement.setString(3, voteType.name());
            statement.setString(4, TimeUtils.toStorage(LocalDateTime.now().withNano(0)));
            statement.executeUpdate();
        }
    }
}
