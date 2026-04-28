package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SuperAdminDAO {

    public int countActiveUsers() {
        return countScalar(
            "SELECT COUNT(*) FROM users u " +
            "JOIN user_roles ur ON u.userID = ur.userID " +
            "JOIN roles r       ON ur.roleID = r.roleID " +
            "WHERE u.isActive = 1 AND r.roleName <> 'customer'");
    }

    public int countActiveCustomers() {
        return countScalar(
            "SELECT COUNT(*) FROM users u " +
            "JOIN user_roles ur ON u.userID = ur.userID " +
            "JOIN roles r       ON ur.roleID = r.roleID " +
            "WHERE u.isActive = 1 AND r.roleName = 'customer'");
    }

    public int countAllUsers() {
        return countScalar("SELECT COUNT(*) FROM users");
    }

    public int countProducts() {
        return countScalar("SELECT COUNT(*) FROM products WHERE status = 'active'");
    }

    public int countOrders() {
        return countScalar("SELECT COUNT(*) FROM orders");
    }

    /** All users with their primary role (first assigned). */
    public List<User> listAllUsers() {
        String sql =
            "SELECT u.userID, u.firstName, u.lastName, u.email, u.username, " +
            "       u.isActive, u.lastLogin, " +
            "       (SELECT r.roleName FROM roles r " +
            "          JOIN user_roles ur ON ur.roleID = r.roleID " +
            "          WHERE ur.userID = u.userID LIMIT 1) AS roleName " +
            "FROM users u ORDER BY u.userID";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.userID    = rs.getInt   ("userID");
                u.firstName = rs.getString("firstName");
                u.lastName  = rs.getString("lastName");
                u.email     = rs.getString("email");
                u.username  = rs.getString("username");
                u.isActive  = rs.getInt   ("isActive");
                u.lastLogin = rs.getString("lastLogin");
                u.roleName  = rs.getString("roleName");
                out.add(u);
            }
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.listAllUsers failed: " + e.getMessage());
        }
        return out;
    }

    public List<String> listRoleNames() {
        List<String> out = new ArrayList<>();
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT roleName FROM roles ORDER BY roleID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString("roleName"));
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.listRoleNames failed: " + e.getMessage());
        }
        return out;
    }

    /** Creates user + role assignment. Returns userID or -1 on failure. */
    public int createUser(String email, String username, String plainPassword,
                          String firstName, String lastName, String roleName) {
        String insertUser =
            "INSERT INTO users (email, username, passwordHash, firstName, lastName, isActive) " +
            "VALUES (?, ?, ?, ?, ?, 1)";
        String insertRole =
            "INSERT INTO user_roles (userID, roleID) " +
            "SELECT ?, roleID FROM roles WHERE roleName = ?";

        try (Connection c = DBConnection.getInstance().getConnection()) {
            c.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = c.prepareStatement(
                    insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, username);
                ps.setString(3, BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)));
                ps.setString(4, firstName);
                ps.setString(5, lastName);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) { c.rollback(); return -1; }
                    userId = keys.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(insertRole)) {
                ps.setInt(1, userId);
                ps.setString(2, roleName);
                ps.executeUpdate();
            }
            c.commit();
            c.setAutoCommit(true);
            return userId;
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.createUser failed: " + e.getMessage());
            return -1;
        }
    }

    public boolean setUserActive(int userID, boolean active) {
        String sql = "UPDATE users SET isActive = ?, updatedAt = CURRENT_TIMESTAMP WHERE userID = ?";
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.setUserActive failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(int userID, String email, String firstName, String lastName, String roleName) {
        String updateUser = "UPDATE users SET email=?, firstName=?, lastName=?, updatedAt=CURRENT_TIMESTAMP WHERE userID=?";
        String deleteRole = "DELETE FROM user_roles WHERE userID=?";
        String insertRole = "INSERT INTO user_roles (userID, roleID) SELECT ?, roleID FROM roles WHERE roleName=?";
        try (Connection c = DBConnection.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(updateUser)) {
                ps.setString(1, email);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setInt(4, userID);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(deleteRole)) {
                ps.setInt(1, userID); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(insertRole)) {
                ps.setInt(1, userID); ps.setString(2, roleName); ps.executeUpdate();
            }
            c.commit();
            c.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.updateUser failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(int userID) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE userID = ?")) {
            ps.setInt(1, userID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SuperAdminDAO.deleteUser failed: " + e.getMessage());
            return false;
        }
    }

    private int countScalar(String sql) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}
