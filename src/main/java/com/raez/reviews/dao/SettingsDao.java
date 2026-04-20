package com.raez.reviews.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.raez.reviews.util.DatabaseManager;

public class SettingsDao {
    private final DatabaseManager databaseManager;

    public SettingsDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int getReviewEditWindowMinutes() {
        String sql = "SELECT settingValue FROM reviews_settings WHERE settingKey = 'review_edit_window_minutes'";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Integer.parseInt(resultSet.getString("settingValue"));
            }
            return 5;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load application settings.", exception);
        }
    }

    public void updateReviewEditWindowMinutes(int minutes) {
        String sql = "UPDATE reviews_settings SET settingValue = ? WHERE settingKey = 'review_edit_window_minutes'";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Integer.toString(minutes));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update application settings.", exception);
        }
    }
}
