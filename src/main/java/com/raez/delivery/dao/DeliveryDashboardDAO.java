package com.raez.delivery.dao;

import com.raez.db.DBConnection;

import java.sql.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliveryDashboardDAO {
    private static final Logger log = LoggerFactory.getLogger(DeliveryDashboardDAO.class);


    public static int getTotalDeliveries()           { return DeliveryDAO.getTotalDeliveries(); }
    public static int getStatusCount(String status)  { return DeliveryDAO.getStatusCount(status); }
    public static List<String> getRecentDeliveries() { return DeliveryDAO.getRecentDeliveries(); }

    public static int getTotalDrivers() {
        String sql = "SELECT COUNT(*) FROM delivery_drivers";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { log.error("Error", e); return 0; }
    }
}
