package com.reaz.customer.dao;

import com.reaz.customer.model.CustomerProfile;
import com.reaz.customer.model.CustomerUser;
import com.reaz.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    // ── LOGIN ──────────────────────────────────────────────────────────────
    public CustomerUser login(String email, String password) throws SQLException {
        String sql =
            "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.userID = u.userID " +
            "JOIN roles r ON r.roleID = ur.roleID " +
            "WHERE u.email = ? AND r.roleName = 'customer' AND u.isActive = 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CustomerUser user = mapUser(rs);
                String hashed = DBConnection.hashPassword(password);
                if (!hashed.equals(user.getPasswordHash())) return null;
                return user;
            }
        }
        return null;
    }

    // ── REGISTER ───────────────────────────────────────────────────────────
    public CustomerUser register(String firstName, String lastName, String email,
                                 String password, String phone, String address,
                                 String idCardPath) throws SQLException {
        if (emailExists(email)) throw new SQLException("Email already registered.");

        String hashedPw  = DBConnection.hashPassword(password);
        String username  = email.split("@")[0] + "_" + System.currentTimeMillis();
        Connection conn  = DBConnection.getInstance().getConnection();

        String insertUser =
            "INSERT INTO users (email, username, passwordHash, firstName, lastName, isActive) " +
            "VALUES (?, ?, ?, ?, ?, 1)";
        int userId;
        try (PreparedStatement ps = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, username);
            ps.setString(3, hashedPw);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) throw new SQLException("User insertion failed.");
            userId = keys.getInt(1);
        }

        String insertRole =
            "INSERT INTO user_roles (userID, roleID) " +
            "SELECT ?, roleID FROM roles WHERE roleName = 'customer'";
        try (PreparedStatement ps = conn.prepareStatement(insertRole)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }

        String insertCustomer =
            "INSERT INTO customers (userID, name, email, contactNumber, deliveryAddress, " +
            "customerType, idCardImage, status) VALUES (?, ?, ?, ?, ?, 'Individual', ?, 'active')";
        try (PreparedStatement ps = conn.prepareStatement(insertCustomer)) {
            ps.setInt(1, userId);
            ps.setString(2, firstName + " " + lastName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setString(5, address);
            ps.setString(6, idCardPath);
            ps.executeUpdate();
        }

        return getById(userId);
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

    // ── GET BY USER ID ─────────────────────────────────────────────────────
    public CustomerUser getById(int userId) throws SQLException {
        String sql =
            "SELECT u.userID, u.email, u.passwordHash, u.firstName, u.lastName, u.isActive, r.roleName " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.userID = u.userID " +
            "JOIN roles r ON r.roleID = ur.roleID " +
            "WHERE u.userID = ? AND r.roleName = 'customer'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }

    // ── GET CUSTOMER PROFILE (from customers table) ────────────────────────
    public CustomerProfile getProfile(int userId) throws SQLException {
        String sql =
            "SELECT customerID, userID, contactNumber, deliveryAddress, idCardImage " +
            "FROM customers WHERE userID = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new CustomerProfile(
                    rs.getInt("customerID"),
                    rs.getInt("userID"),
                    rs.getString("contactNumber"),
                    rs.getString("deliveryAddress"),
                    rs.getString("idCardImage")
                );
            }
        }
        return null;
    }

    // ── SAVE PROFILE ───────────────────────────────────────────────────────
    public void saveProfile(int userId, String firstName, String lastName,
                            String phone, String address) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET firstName=?, lastName=?, updatedAt=datetime('now') WHERE userID=?")) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customers SET name=?, contactNumber=?, deliveryAddress=? WHERE userID=?")) {
            ps.setString(1, (firstName + " " + lastName).trim());
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    // ── TOGGLE ACCOUNT STATUS ──────────────────────────────────────────────
    public void toggleAccountStatus(int userId) throws SQLException {
        String sql = "UPDATE users SET isActive = CASE WHEN isActive=1 THEN 0 ELSE 1 END WHERE userID=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ── EMAIL EXISTS ───────────────────────────────────────────────────────
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
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
