package com.reaz.customer.dao;

import com.reaz.customer.model.CustomerPreference;
import com.reaz.db.DBConnection;

import java.sql.*;

public class CustomerPreferenceDAO {

    // ── GET BY USER ID (joins customers → customer_preferences) ───────────
    public CustomerPreference getByUserId(int userId) throws SQLException {
        String sql =
            "SELECT cp.preferenceID, cp.customerID, cp.preferredCategories, " +
            "       cp.notificationSettings, cp.deliveryInstructions " +
            "FROM customer_preferences cp " +
            "JOIN customers c ON c.customerID = cp.customerID " +
            "WHERE c.userID = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CustomerPreference p = new CustomerPreference();
                p.setPreferenceId(rs.getInt("preferenceID"));
                p.setCustomerId(rs.getInt("customerID"));
                p.setPreferredCategories(rs.getString("preferredCategories"));
                p.setNotificationSettings(rs.getString("notificationSettings"));
                p.setDeliveryInstructions(rs.getString("deliveryInstructions"));
                return p;
            }
        }
        return null;
    }

    // ── SAVE (upsert) ──────────────────────────────────────────────────────
    public void savePreferences(int userId, String categories, String notifications,
                                String deliveryInstructions) throws SQLException {
        // Resolve customerID from userID
        Integer customerId = getCustomerIdByUserId(userId);
        if (customerId == null) return;

        String sql =
            "INSERT OR REPLACE INTO customer_preferences " +
            "(customerID, preferredCategories, notificationSettings, deliveryInstructions) " +
            "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, categories);
            ps.setString(3, notifications);
            ps.setString(4, deliveryInstructions);
            ps.executeUpdate();
        }
    }

    private Integer getCustomerIdByUserId(int userId) throws SQLException {
        String sql = "SELECT customerID FROM customers WHERE userID = ? LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("customerID");
        }
        return null;
    }
}
