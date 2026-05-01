package com.raez.delivery.dao;

import com.raez.db.DBConnection;
import com.raez.delivery.model.DeliveryDelivery;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliveryDAO {
    private static final Logger log = LoggerFactory.getLogger(DeliveryDAO.class);


    public static List<String> getAllDriverIds() {
        List<String> driverIds = new ArrayList<>();
        String sql = "SELECT driverID FROM delivery_drivers ORDER BY driverID";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) driverIds.add(String.valueOf(rs.getInt("driverID")));
        } catch (SQLException e) { log.error("Error", e); }
        return driverIds;
    }

    public static List<DeliveryDelivery> getAllDeliveries() {
        List<DeliveryDelivery> deliveries = new ArrayList<>();
        String sql = "SELECT d.deliveryID, d.orderID, d.customerAddress, d.orderStatus, " +
                     "d.orderDate, d.numOfItems, COALESCE(d.driverID, 0) AS driverID " +
                     "FROM delivery_deliveries d ORDER BY d.deliveryID";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                deliveries.add(new DeliveryDelivery(
                        safe(rs.getString("deliveryID")),
                        safe(rs.getString("orderID")),
                        safe(rs.getString("customerAddress")),
                        safe(rs.getString("orderStatus")),
                        safe(rs.getString("orderDate")),
                        String.valueOf(rs.getInt("numOfItems")),
                        String.valueOf(rs.getInt("driverID"))
                ));
            }
        } catch (SQLException e) { log.error("Error", e); }
        return deliveries;
    }

    public static boolean addDelivery(DeliveryDelivery delivery) {
        // deliveryID is AUTOINCREMENT in the unified schema — omit it
        String sql = "INSERT INTO delivery_deliveries " +
                     "(orderID, customerAddress, orderStatus, orderDate, numOfItems, driverID, warehouseID) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NULL)";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setInt(1,    parseIntSafe(delivery.getOrderId(), 1));
            stmt.setString(2, delivery.getCustomerAddress());
            stmt.setString(3, delivery.getOrderStatus());
            stmt.setString(4, delivery.getOrderDate());
            stmt.setInt(5,    parseIntSafe(delivery.getNumOfItems(), 1));
            int driverId = parseIntSafe(delivery.getDriverId(), 0);
            if (driverId > 0) stmt.setInt(6, driverId); else stmt.setNull(6, Types.INTEGER);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { log.error("Error", e); return false; }
    }

    public static int getTotalDeliveries() {
        String sql = "SELECT COUNT(*) FROM delivery_deliveries";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { log.error("Error", e); return 0; }
    }

    public static int getStatusCount(String status) {
        String sql = "SELECT COUNT(*) FROM delivery_deliveries WHERE LOWER(orderStatus) = LOWER(?)";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { log.error("Error", e); return 0; }
    }

    public static List<String> getRecentDeliveries() {
        List<String> recent = new ArrayList<>();
        String sql = "SELECT deliveryID, orderID, orderStatus FROM delivery_deliveries " +
                     "ORDER BY deliveryID DESC LIMIT 5";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                recent.add(safe(rs.getString("deliveryID")) + " - "
                         + safe(rs.getString("orderID")) + " - "
                         + safe(rs.getString("orderStatus")));
            }
        } catch (SQLException e) { log.error("Error", e); }
        return recent;
    }

    private static String safe(String v) { return v == null ? "" : v; }
    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
