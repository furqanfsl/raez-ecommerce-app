package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    public static boolean isValidLogin(String email, String password) {
    	String sql = "SELECT u.userID, u.passwordHash FROM users u JOIN user_roles ur ON ur.userID = u.userID JOIN roles r ON r.roleID = ur.roleID WHERE u.email = ? AND r.roleName IN ('delivery_user','delivery_admin','super_admin') LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}