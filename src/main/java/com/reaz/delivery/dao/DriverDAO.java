package com.reaz.delivery.dao;

import com.reaz.db.DBConnection;
import com.reaz.delivery.model.DeliveryDriver;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {

    public static List<DeliveryDriver> getAllDrivers() {
        List<DeliveryDriver> drivers = new ArrayList<>();
        String sql = "SELECT driverID, driverName, licenceNumber, phoneNum, email " +
                     "FROM delivery_drivers ORDER BY driverID";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                drivers.add(new DeliveryDriver(
                        String.valueOf(rs.getInt("driverID")),
                        safe(rs.getString("driverName")),
                        safe(rs.getString("licenceNumber")),
                        safe(rs.getString("phoneNum")),
                        safe(rs.getString("email"))
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return drivers;
    }

    public static List<String> getAllDriverIds() {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT driverID FROM delivery_drivers ORDER BY driverID";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) ids.add(String.valueOf(rs.getInt("driverID")));
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    public static boolean addDriver(DeliveryDriver driver) {
        String sql = "INSERT INTO delivery_drivers (licenceNumber, phoneNum, email, driverName) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, driver.getLicenseNumber());
            stmt.setString(2, driver.getPhone());
            stmt.setString(3, driver.getEmail());
            stmt.setString(4, driver.getName());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean deleteDriver(String driverId) {
        String sql = "DELETE FROM delivery_drivers WHERE driverID = ?";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(driverId));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private static String safe(String v) { return v == null ? "" : v; }
}
