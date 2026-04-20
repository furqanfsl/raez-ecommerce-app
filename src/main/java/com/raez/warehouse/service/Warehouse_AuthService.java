package com.raez.warehouse.service;

import com.raez.db.DBConnection;
import com.raez.warehouse.util.Warehouse_ValidationUtil;

import java.sql.*;

public class Warehouse_AuthService {

    public AuthResult authenticateStaff(String email, String password) {
        if (email == null || email.isBlank())
            return AuthResult.fail("Please enter your email.");
        if (password == null || password.isBlank())
            return AuthResult.fail("Please enter your password.");
        if (!Warehouse_ValidationUtil.isValidStaffEmail(email))
            return AuthResult.fail("Access denied: Only staff email accounts (@raez.org.uk) are allowed.");

        String hashedPassword = DBConnection.hashPassword(password);
        String sql = "SELECT u.userID, r.roleName " +
                     "FROM users u " +
                     "JOIN user_roles ur ON ur.userID = u.userID " +
                     "JOIN roles r ON r.roleID = ur.roleID " +
                     "WHERE u.email = ? AND u.passwordHash = ? AND u.isActive = 1 " +
                     "AND r.roleName IN ('warehouse_admin', 'super_admin')";

        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                return AuthResult.fail("Invalid email or password.");

            return AuthResult.success(email.trim(), rs.getString("roleName"), rs.getInt("userID"));

        } catch (SQLException e) {
            System.err.println("AuthService.authenticateStaff error: " + e.getMessage());
            return AuthResult.fail("Database error. Please try again.");
        }
    }

    public AuthResult authenticateUser(String email, String password) {
        if (email == null || email.isBlank())
            return AuthResult.fail("Please enter your email.");
        if (password == null || password.isBlank())
            return AuthResult.fail("Please enter your password.");

        Warehouse_ValidationUtil.ValidationResult emailCheck = Warehouse_ValidationUtil.validateUserEmail(email);
        if (!emailCheck.isSuccess())
            return AuthResult.fail(emailCheck.getMessage());

        String hashedPassword = DBConnection.hashPassword(password);
        String sql = "SELECT u.userID, r.roleName " +
                     "FROM users u " +
                     "JOIN user_roles ur ON ur.userID = u.userID " +
                     "JOIN roles r ON r.roleID = ur.roleID " +
                     "WHERE u.email = ? AND u.passwordHash = ? AND u.isActive = 1 " +
                     "AND r.roleName IN ('warehouse_user', 'warehouse_admin', 'super_admin')";

        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                return AuthResult.fail("Invalid email or password. " +
                        "Only authorised warehouse users can access this portal.");

            return AuthResult.success(email.trim(), rs.getString("roleName"), rs.getInt("userID"));

        } catch (SQLException e) {
            System.err.println("AuthService.authenticateUser error: " + e.getMessage());
            return AuthResult.fail("Database error. Please try again.");
        }
    }

    public static class AuthResult {
        private final boolean success;
        private final String  message;
        private final String  email;
        private final String  role;
        private final int     userID;

        private AuthResult(boolean success, String message, String email, String role, int userID) {
            this.success = success;
            this.message = message;
            this.email   = email;
            this.role    = role;
            this.userID  = userID;
        }

        public static AuthResult success(String email, String role, int userID) {
            return new AuthResult(true, null, email, role, userID);
        }

        public static AuthResult fail(String message) {
            return new AuthResult(false, message, null, null, -1);
        }

        public boolean isSuccess()  { return success; }
        public String  getMessage() { return message; }
        public String  getEmail()   { return email; }
        public String  getRole()    { return role; }
        public int     getUserID()  { return userID; }
    }
}
