package com.raez.delivery.dao;

import com.raez.db.DBConnection;

import java.sql.*;
import java.util.List;

public class DeliveryDashboardDAO {

    public static int getTotalDeliveries()           { return DeliveryDAO.getTotalDeliveries(); }
    public static int getStatusCount(String status)  { return DeliveryDAO.getStatusCount(status); }
    public static List<String> getRecentDeliveries() { return DeliveryDAO.getRecentDeliveries(); }

    public static int getTotalDrivers() {
        String sql = "SELECT COUNT(*) FROM delivery_drivers";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }
}
