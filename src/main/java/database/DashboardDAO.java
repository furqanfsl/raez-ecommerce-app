package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DashboardDAO {

    public static int getTotalDeliveries() {
        return DeliveryDAO.getTotalDeliveries();
    }

    public static int getTotalDrivers() {
        String sql = "SELECT COUNT(*) FROM Driver";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getStatusCount(String status) {
        return DeliveryDAO.getStatusCount(status);
    }

    public static List<String> getRecentDeliveries() {
        return DeliveryDAO.getRecentDeliveries();
    }
}