package com.raez.delivery.dao;

import com.raez.db.DBConnection;
import com.raez.delivery.model.DeliveryDriver;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverDAO {
    private static final Logger log = LoggerFactory.getLogger(DriverDAO.class);


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
        } catch (SQLException e) { log.error("Error", e); }
        return drivers;
    }

    public static List<String> getAllDriverIds() {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT driverID FROM delivery_drivers ORDER BY driverID";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) ids.add(String.valueOf(rs.getInt("driverID")));
        } catch (SQLException e) { log.error("Error", e); }
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
        } catch (SQLException e) { log.error("Error", e); return false; }
    }

    public static boolean deleteDriver(String driverId) {
        String sql = "DELETE FROM delivery_drivers WHERE driverID = ?";
        try (PreparedStatement stmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(driverId));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { log.error("Error", e); return false; }
    }

    private static String safe(String v) { return v == null ? "" : v; }
}
