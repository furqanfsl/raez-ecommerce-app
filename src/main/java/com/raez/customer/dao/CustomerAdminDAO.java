package com.raez.customer.dao;

import com.raez.customer.model.CustomerUser;
import com.raez.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerAdminDAO {

    // ── STAFF LOGIN (Customer module: super_admin or customer_admin) ─────────
    public CustomerUser staffLogin(String email, String password) throws SQLException {
        CustomerUser u = loginWithRole(email, password, "super_admin");
        if (u != null) return u;
        return loginWithRole(email, password, "customer_admin");
    }

    // ── SUPER ADMIN LOGIN ──────────────────────────────────────────────────
    public CustomerUser superAdminLogin(String email, String password) throws SQLException {
        return loginWithRole(email, password, "super_admin");
    }

    private CustomerUser loginWithRole(String email, String password,
                                       String roleName) throws SQLException {
        String sql =
            "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.userID = u.userID " +
            "JOIN roles r ON r.roleID = ur.roleID " +
            "WHERE u.email = ? AND u.passwordHash = ? AND r.roleName = ? AND u.isActive = 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, DBConnection.hashPassword(password));
            ps.setString(3, roleName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    // ── GET ALL CUSTOMERS ──────────────────────────────────────────────────
    public List<CustomerUser> getAllCustomers() throws SQLException {
        String sql =
            "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.userID = u.userID " +
            "JOIN roles r ON r.roleID = ur.roleID " +
            "WHERE r.roleName = 'customer'";
        List<CustomerUser> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    // ── SEARCH CUSTOMERS ───────────────────────────────────────────────────
    public List<CustomerUser> searchCustomers(String keyword, String status) throws SQLException {
        boolean allStatuses = "All".equals(status);
        String sql;
        if (allStatuses) {
            sql =
                "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
                "FROM users u " +
                "JOIN user_roles ur ON ur.userID = u.userID " +
                "JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE r.roleName = 'customer' " +
                "  AND (u.email LIKE ? OR u.username LIKE ? OR CAST(u.userID AS TEXT) LIKE ?)";
        } else {
            int isActive = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;
            sql =
                "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
                "FROM users u " +
                "JOIN user_roles ur ON ur.userID = u.userID " +
                "JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE r.roleName = 'customer' AND u.isActive = " + isActive +
                "  AND (u.email LIKE ? OR u.username LIKE ? OR CAST(u.userID AS TEXT) LIKE ?)";
        }
        String search = "%" + keyword + "%";
        List<CustomerUser> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, search);
            ps.setString(2, search);
            ps.setString(3, search);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    // ── ANALYTICS ─────────────────────────────────────────────────────────
    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(totalAmount), 0) FROM orders";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }

    public int countByStatus(String status) throws SQLException {
        int isActive = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;
        String sql =
            "SELECT COUNT(*) FROM users u " +
            "JOIN user_roles ur ON ur.userID = u.userID " +
            "JOIN roles r ON r.roleID = ur.roleID " +
            "WHERE r.roleName = 'customer' AND u.isActive = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isActive);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public double getTotalSpentByUserId(int userId) throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(o.totalAmount), 0) FROM orders o " +
            "JOIN customers c ON c.customerID = o.customerID WHERE c.userID = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }

    // ── EDIT PROFILE (admin-level, logs change) ────────────────────────────
    public void updateProfile(int targetUserId, String firstName, String lastName,
                              String phone, String address,
                              int adminUserId) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        String oldName = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT firstName, lastName FROM users WHERE userID = ?")) {
            ps.setInt(1, targetUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) oldName = rs.getString("firstName") + " " + rs.getString("lastName");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET firstName=?, lastName=?, updatedAt=datetime('now') WHERE userID=?")) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setInt(3, targetUserId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customers SET name=?, contactNumber=?, deliveryAddress=? WHERE userID=?")) {
            ps.setString(1, (firstName + " " + lastName).trim());
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setInt(4, targetUserId);
            ps.executeUpdate();
        }
        if (oldName != null) {
            logChange(adminUserId, targetUserId, "name",
                      oldName.trim(), (firstName + " " + lastName).trim());
        }
    }

    // ── UPDATE EMAIL (super-admin only, logs change) ───────────────────────
    public void updateCustomerEmail(int customerId, String newEmail,
                                    int adminUserId) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        String oldEmail = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT email FROM customers WHERE customerID = ?")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) oldEmail = rs.getString("email");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customers SET email = ? WHERE customerID = ?")) {
            ps.setString(1, newEmail);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
        if (oldEmail != null) {
            logChangeByCustomerId(adminUserId, customerId, "email", oldEmail, newEmail);
        }
    }

    // ── UPDATE ID CARD (super-admin only, logs change) ─────────────────────
    public void updateCustomerIdCard(int customerId, String newIdCard,
                                     int adminUserId) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        String oldIdCard = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT idCardImage FROM customers WHERE customerID = ?")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) oldIdCard = rs.getString("idCardImage");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customers SET idCardImage = ? WHERE customerID = ?")) {
            ps.setString(1, newIdCard);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
        if (oldIdCard != null) {
            logChangeByCustomerId(adminUserId, customerId, "idCardImage", oldIdCard, newIdCard);
        }
    }

    // ── GET CUSTOMER BY EMAIL (for super-admin panel search) ───────────────
    public int[] getCustomerByEmail(String email) throws SQLException {
        // Returns [customerID, userID] or null if not found
        String sql = "SELECT customerID, userID FROM customers WHERE email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new int[]{rs.getInt("customerID"), rs.getInt("userID")};
            }
        }
        return null;
    }

    // ── TOGGLE CUSTOMER STATUS ─────────────────────────────────────────────
    public void toggleCustomerStatus(int userId, int adminUserId) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        int oldActive;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT isActive FROM users WHERE userID = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            oldActive = rs.next() ? rs.getInt("isActive") : 1;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET isActive = CASE WHEN isActive=1 THEN 0 ELSE 1 END WHERE userID=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        logChange(adminUserId, userId, "isActive",
                  String.valueOf(oldActive), oldActive == 1 ? "0" : "1");
    }

    // ── AUDIT LOG helpers ──────────────────────────────────────────────────
    public void logChange(int adminUserId, int targetUserId,
                          String field, String oldVal, String newVal) throws SQLException {
        Integer customerId = getCustomerIdByUserId(targetUserId);
        if (customerId == null) return;
        logChangeByCustomerId(adminUserId, customerId, field, oldVal, newVal);
    }

    private void logChangeByCustomerId(int adminUserId, int customerId,
                                       String field, String oldVal, String newVal)
            throws SQLException {
        String sql =
            "INSERT INTO customer_updates (adminUserID, customerID, updatedField, oldValue, newValue, updateDate) " +
            "VALUES (?, ?, ?, ?, ?, datetime('now'))";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminUserId);
            ps.setInt(2, customerId);
            ps.setString(3, field);
            ps.setString(4, oldVal);
            ps.setString(5, newVal);
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

    // ── MAPPER ─────────────────────────────────────────────────────────────
    private CustomerUser mapUser(ResultSet rs) throws SQLException {
        CustomerUser u = new CustomerUser();
        u.setId(rs.getInt("userID"));
        String first = rs.getString("firstName");
        String last  = rs.getString("lastName");
        String full  = ((first != null ? first : "") +
                        (last != null && !last.isBlank() ? " " + last : "")).trim();
        u.setName(full.isEmpty() ? rs.getString("email") : full);
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("passwordHash"));
        u.setRole(rs.getString("roleName"));
        u.setStatus(rs.getInt("isActive") == 1 ? "ACTIVE" : "INACTIVE");
        return u;
    }
}
