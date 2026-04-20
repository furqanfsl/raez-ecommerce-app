package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.raez.reviews.model.ModerationAuditEntry;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.TimeUtils;

public class ModerationAuditDao {
    private final DatabaseManager databaseManager;

    public ModerationAuditDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void insert(Connection connection, int reviewId, int adminUserId, String action, String reason, String actionTime)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO reviews_moderation(reviewID, adminUserID, action, reason, actionTime) VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, reviewId);
            statement.setInt(2, adminUserId);
            statement.setString(3, action);
            statement.setString(4, reason);
            statement.setString(5, actionTime);
            statement.executeUpdate();
        }
    }

    public List<ModerationAuditEntry> findAll() {
        String sql = """
                SELECT rm.auditID, rm.reviewID, rm.adminUserID,
                       COALESCE(NULLIF(TRIM(COALESCE(u.firstName, '') || ' ' || COALESCE(u.lastName, '')), ''), u.username) AS adminName,
                       rm.action, rm.reason, rm.actionTime
                FROM reviews_moderation rm
                JOIN users u ON u.userID = rm.adminUserID
                ORDER BY rm.actionTime DESC
                """;
        List<ModerationAuditEntry> entries = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                entries.add(new ModerationAuditEntry(
                        resultSet.getInt("auditID"),
                        resultSet.getInt("reviewID"),
                        resultSet.getInt("adminUserID"),
                        resultSet.getString("adminName"),
                        resultSet.getString("action"),
                        resultSet.getString("reason"),
                        TimeUtils.fromStorage(resultSet.getString("actionTime"))));
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load moderation audit history.", exception);
        }
    }
}
