package com.raez.finance.dao;

import com.raez.finance.model.FinanceUser;
import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.util.FinanceDatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class FinanceUserDao {

    public List<FinanceUser> findAll() throws Exception {
        String sql = "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                "COALESCE(r.roleName,'finance_user') AS role, " +
                "u.firstName, u.lastName, u.phone, u.isActive, u.lastLogin " +
                "FROM users u " +
                "LEFT JOIN user_roles ur ON ur.userID = u.userID " +
                "LEFT JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE r.roleName IN ('finance_user','finance_admin','super_admin') " +
                "ORDER BY u.username";
        List<FinanceUser> list = new ArrayList<>();
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Finds a user by email or username (case-insensitive trim on both columns).
     * Returns null if not found.
     */
    public FinanceUser findByEmailOrUsername(String lookup) throws Exception {
        if (lookup == null || lookup.isBlank()) return null;
        String settingKey = lookup.trim();
        String sql = "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                "COALESCE(r.roleName,'finance_user') AS role, " +
                "u.firstName, u.lastName, u.phone, u.isActive, u.lastLogin " +
                "FROM users u " +
                "LEFT JOIN user_roles ur ON ur.userID = u.userID " +
                "LEFT JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE (TRIM(LOWER(u.email)) = TRIM(LOWER(?)) OR TRIM(LOWER(u.username)) = TRIM(LOWER(?))) " +
                "AND r.roleName IN ('finance_user','finance_admin','super_admin')";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, settingKey);
            ps.setString(2, settingKey);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Finds a user by email (case-insensitive trim). Returns null if not found. */
    public FinanceUser findByEmail(String email) throws Exception {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                "COALESCE(r.roleName,'finance_user') AS role, " +
                "u.firstName, u.lastName, u.phone, u.isActive, u.lastLogin " +
                "FROM users u " +
                "LEFT JOIN user_roles ur ON ur.userID = u.userID " +
                "LEFT JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE TRIM(LOWER(u.email)) = TRIM(LOWER(?)) " +
                "AND r.roleName IN ('finance_user','finance_admin','super_admin')";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Finds a user by ID. Returns null if not found. */
    public FinanceUser findById(int userId) throws Exception {
        String sql = "SELECT u.userID, u.email, u.username, u.passwordHash, " +
                "COALESCE(r.roleName,'finance_user') AS role, " +
                "u.firstName, u.lastName, u.phone, u.isActive, u.lastLogin " +
                "FROM users u " +
                "LEFT JOIN user_roles ur ON ur.userID = u.userID " +
                "LEFT JOIN roles r ON r.roleID = ur.roleID " +
                "WHERE u.userID = ? " +
                "AND r.roleName IN ('finance_user','finance_admin','super_admin')";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Returns the createdAt timestamp for the user, or null if not found. */
    public String getCreatedAt(int userId) throws Exception {
        String sql = "SELECT createdAt FROM users WHERE userID = ?";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("createdAt") : null;
        }
    }

    private FinanceUser mapRow(ResultSet rs) throws Exception {
        int id = rs.getInt("userID");
        String email = rs.getString("email");
        String username = rs.getString("username");
        String passwordHash = rs.getString("passwordHash");
        String roleStr = rs.getString("role");
        String normalized = roleStr == null ? null : roleStr.trim().toUpperCase().replace(" ", "_");
        // Backwards compatibility with older seed/test data: `role='USER'`
        // should behave like a finance user.
        FinanceUserRole role;
        if (normalized == null || normalized.isBlank()) {
            role = FinanceUserRole.FINANCE_USER;
        } else if ("USER".equals(normalized) || "FINANCE".equals(normalized)
                || "FINANCEUSER".equals(normalized) || "FINANCE_USER".equals(normalized)) {
            role = FinanceUserRole.FINANCE_USER;
        } else if ("FINANCE_ADMIN".equals(normalized) || "SUPER_ADMIN".equals(normalized)) {
            role = FinanceUserRole.ADMIN;
        } else {
            try {
                role = FinanceUserRole.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                role = FinanceUserRole.FINANCE_USER;
            }
        }
        String firstName = rs.getString("firstName");
        String lastName = rs.getString("lastName");
        String phone = rs.getString("phone");
        boolean active = rs.getInt("isActive") == 1;
        LocalDateTime lastLogin = parseLastLogin(rs.getString("lastLogin"));
        String staffID = null;
        String a1 = null;
        String a2 = null;
        String a3 = null;
        return new FinanceUser(id, email, username, passwordHash, role, firstName, lastName, phone,
            staffID, a1, a2, a3, active, lastLogin);
    }

    private static String safeCol(ResultSet rs, String col) {
        try {
            String s = rs.getString(col);
            return rs.wasNull() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseLastLogin(String lastLoginStr) {
        if (lastLoginStr == null || lastLoginStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(lastLoginStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(lastLoginStr);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    public void insertUser(
            String email,
            String username,
            String passwordHash,
            String role,
            String firstName,
            String lastName,
            String phone,
            String staffID,
            String addressLine1,
            String addressLine2,
            String addressLine3,
            boolean isActive
    ) throws Exception {
        String insertUserSql = "INSERT INTO users (email, username, passwordHash, firstName, lastName, phone, isActive, lastLogin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";
        String roleLookupSql = "SELECT roleID FROM roles WHERE roleName = ?";
        String assignRoleSql = "INSERT OR IGNORE INTO user_roles (userID, roleID) VALUES (?, ?)";

        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement insUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement roleLookup = conn.prepareStatement(roleLookupSql);
             PreparedStatement assignRole = conn.prepareStatement(assignRoleSql)) {
            insUser.setString(1, email);
            insUser.setString(2, username);
            insUser.setString(3, passwordHash);
            insUser.setString(4, firstName);
            insUser.setString(5, lastName);
            insUser.setString(6, phone);
            insUser.setInt(7, isActive ? 1 : 0);
            insUser.executeUpdate();
            int userId;
            try (ResultSet keys = insUser.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalArgumentException("Failed to create user");
                userId = keys.getInt(1);
            }
            String mappedRole = "ADMIN".equalsIgnoreCase(role) ? "finance_admin" : "finance_user";
            roleLookup.setString(1, mappedRole);
            try (ResultSet rs = roleLookup.executeQuery()) {
                if (rs.next()) {
                    assignRole.setInt(1, userId);
                    assignRole.setInt(2, rs.getInt("roleID"));
                    assignRole.executeUpdate();
                }
            }
        }
    }

    /**
     * Deletes a user by ID. Caller should confirm with the user before invoking.
     */
    public void deleteByUserId(int userId) throws Exception {
        String sql = "DELETE FROM users WHERE userID = ?";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new IllegalArgumentException("FinanceUser not found");
            }
        }
    }

    /**
     * Updates the password hash for the given user (e.g. after "Update Password" in My Account).
     */
    public void updatePasswordByUserId(int userId, String newPasswordHash) throws Exception {
        String sql = "UPDATE users SET passwordHash = ? WHERE userID = ?";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("FinanceUser not found");
            }
        }
    }

    /**
     * Sets a temporary password and clears lastLogin so the next login is treated as first-time (must change password).
     * Used for forgot-password flow on the login screen.
     */
    public void setTemporaryPasswordAndClearLastLogin(int userId, String newPasswordHash) throws Exception {
        String sql = "UPDATE users SET passwordHash = ?, lastLogin = NULL WHERE userID = ?";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("FinanceUser not found");
            }
        }
    }

    /**
     * Updates user profile fields (email, username, firstName, lastName, phone, role, isActive).
     * Does not change password or lastLogin.
     */
    public void updateUser(int userId, String email, String username, String firstName, String lastName,
                           String phone, String staffID, String addressLine1, String addressLine2, String addressLine3,
                           FinanceUserRole role, boolean isActive) throws Exception {
        String sql = "UPDATE users SET email = ?, username = ?, firstName = ?, lastName = ?, phone = ?, isActive = ? WHERE userID = ?";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, username);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, phone);
            ps.setInt(6, isActive ? 1 : 0);
            ps.setInt(7, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("FinanceUser not found");
            }
        }
    }
}
