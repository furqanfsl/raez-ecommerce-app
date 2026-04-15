package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.Delivery;

public class DeliveryDAO {
	
	public static List<String> getAllDriverIds() {
	    List<String> driverIds = new ArrayList<>();

	    String sql = "SELECT driverID FROM Driver ORDER BY driverID";

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

    public static List<Delivery> getAllDeliveries() {
        List<Delivery> deliveries = new ArrayList<>();

        String sql = "SELECT DeliveryID, OrderID, customerAddress, orderStatus, orderDate, numOfItems, driverID "
                   + "FROM Delivery "
                   + "ORDER BY DeliveryID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                deliveries.add(new Delivery(
                        safe(rs.getString("DeliveryID")),
                        safe(rs.getString("OrderID")),
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

    public static boolean addDelivery(Delivery delivery) {
        String sql = "INSERT INTO Delivery "
                   + "(DeliveryID, OrderID, customerAddress, orderStatus, orderDate, numOfItems, driverID) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

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
        String sql = "SELECT MAX(CAST(SUBSTR(DeliveryID, 5) AS INTEGER)) "
                   + "FROM Delivery WHERE DeliveryID LIKE 'DEL-%'";

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
        String sql = "SELECT MAX(CAST(SUBSTR(OrderID, 5) AS INTEGER)) "
                   + "FROM Delivery WHERE OrderID LIKE 'ORD-%'";

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
        String sql = "SELECT COUNT(*) FROM Delivery";

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
        String sql = "SELECT COUNT(*) FROM Delivery WHERE LOWER(orderStatus) = LOWER(?)";

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

        String sql = "SELECT DeliveryID, OrderID, orderStatus "
                   + "FROM Delivery "
                   + "ORDER BY orderDate DESC "
                   + "LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                recent.add(
                        safe(rs.getString("DeliveryID")) + " - "
                      + safe(rs.getString("OrderID")) + " - "
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