package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.delivery_deliveries;

public class DeliveryDAO {
	
	public static List<String> getAllDriverIds() {
	    List<String> driverIds = new ArrayList<>();

	    String sql = "SELECT driverID FROM delivery_drivers ORDER BY driverID";

	    try (Connection conn = DBConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(sql);
	         ResultSet rs = stmt.executeQuery()) {

	        while (rs.next()) {
	            driverIds.add(String.valueOf(rs.getInt("driverID")));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return driverIds;
	}

    public static List<delivery_deliveries> getAllDeliveries() {
        List<delivery_deliveries> deliveries = new ArrayList<>();

        String sql = "SELECT deliveryID, orderID, customerAddress, orderStatus, orderDate, numOfItems, driverID "
                   + "FROM delivery_deliveries "
                   + "ORDER BY deliveryID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                deliveries.add(new delivery_deliveries(
                        safe(rs.getString("deliveryID")),
                        safe(rs.getString("orderID")),
                        safe(rs.getString("customerAddress")),
                        safe(rs.getString("orderStatus")),
                        safe(rs.getString("orderDate")),
                        String.valueOf(rs.getInt("numOfItems")),
                        String.valueOf(rs.getInt("driverID"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deliveries;
    }

    public static boolean addDelivery(delivery_deliveries delivery) {
        String sql = "INSERT INTO delivery_deliveries "
                   + "(deliveryID, orderID, customerAddress, orderStatus, orderDate, numOfItems, driverID, warehouseID) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, delivery.getDeliveryId());
            stmt.setString(2, delivery.getOrderId());
            stmt.setString(3, delivery.getCustomerAddress());
            stmt.setString(4, delivery.getOrderStatus());
            stmt.setString(5, delivery.getOrderDate());
            stmt.setInt(6, Integer.parseInt(delivery.getNumOfItems()));
            stmt.setInt(7, Integer.parseInt(delivery.getDriverId()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getNextDeliveryId() {
        String sql = "SELECT MAX(CAST(SUBSTR(deliveryID, 5) AS INTEGER)) "
                   + "FROM delivery_deliveries WHERE deliveryID LIKE 'DEL-%'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int next = 1;
            if (rs.next()) {
                next = rs.getInt(1) + 1;
            }
            return String.format("DEL-%03d", next);

        } catch (SQLException e) {
            e.printStackTrace();
            return "DEL-001";
        }
    }

    public static String getNextOrderId() {
        String sql = "SELECT MAX(CAST(SUBSTR(orderID, 5) AS INTEGER)) "
                   + "FROM delivery_deliveries WHERE orderID LIKE 'ORD-%'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int next = 1;
            if (rs.next()) {
                next = rs.getInt(1) + 1;
            }
            return String.format("ORD-%03d", next);

        } catch (SQLException e) {
            e.printStackTrace();
            return "ORD-001";
        }
    }

    public static int getTotalDeliveries() {
        String sql = "SELECT COUNT(*) FROM delivery_deliveries";

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
        String sql = "SELECT COUNT(*) FROM delivery_deliveries WHERE LOWER(orderStatus) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static List<String> getRecentDeliveries() {
        List<String> recent = new ArrayList<>();

        String sql = "SELECT deliveryID, orderID, orderStatus "
                   + "FROM delivery_deliveries "
                   + "ORDER BY orderDate DESC "
                   + "LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                recent.add(
                        safe(rs.getString("deliveryID")) + " - "
                      + safe(rs.getString("orderID")) + " - "
                      + safe(rs.getString("orderStatus"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recent;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}	