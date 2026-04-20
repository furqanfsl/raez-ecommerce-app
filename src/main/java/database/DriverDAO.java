package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.delivery_drivers;

public class DriverDAO {

    public static List<delivery_drivers> getAllDrivers() {
        List<delivery_drivers> drivers = new ArrayList<>();

        String sql = "SELECT driverID, driverName, licenceNumber, phoneNum, email FROM delivery_drivers ORDER BY driverID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                drivers.add(new delivery_drivers(
                        String.valueOf(rs.getInt("driverID")),
                        safe(rs.getString("driverName")),
                        safe(rs.getString("licenceNumber")),
                        safe(rs.getString("phoneNum")),
                        safe(rs.getString("email"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return drivers;
    }

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

    public static boolean addDriver(delivery_drivers driver) {
        String sql = "INSERT INTO delivery_drivers (licenceNumber, phoneNum, email, driverName) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, driver.getLicenseNumber());
            stmt.setString(2, driver.getPhone());
            stmt.setString(3, driver.getEmail());
            stmt.setString(4, driver.getName());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteDriver(String driverId) {
        String sql = "DELETE FROM delivery_drivers WHERE driverID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(driverId));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}