package com.reaz.warehouse.service;

import com.reaz.db.DBConnection;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

/**
 * DeliveryService — handles loading, confirming and rejecting deliveries.
 * CHANGED: Updated all table names to match unified database schema.
 */
public class Warehouse_DeliveryService {

    public ObservableList<DeliveryRow> loadPendingDeliveries() {
        ObservableList<DeliveryRow> result = FXCollections.observableArrayList();

        // CHANGED: Delivery → delivery_deliveries, Driver → delivery_drivers, Warehouse → warehouse_warehouses
        String sql = "SELECT d.deliveryID, d.orderID, d.customerAddress, " +
                     "d.numOfItems, d.orderDate, d.warehouseID, " +
                     "COALESCE(dr.driverName, 'Unassigned') AS driverName, " +
                     "COALESCE(w.warehouseName, 'Unknown') AS warehouseName " +
                     "FROM delivery_deliveries d " +
                     "LEFT JOIN delivery_drivers dr ON d.driverID = dr.driverID " +
                     "LEFT JOIN warehouse_warehouses w ON d.warehouseID = w.warehouseID " +
                     "WHERE d.orderStatus = 'Pending' " +
                     "ORDER BY d.deliveryID DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(new DeliveryRow(
                        rs.getInt("deliveryID"),
                        rs.getInt("orderID"),
                        rs.getString("customerAddress") != null ? rs.getString("customerAddress") : "N/A",
                        rs.getInt("numOfItems"),
                        rs.getString("driverName"),
                        rs.getString("orderDate") != null ? rs.getString("orderDate") : "N/A",
                        rs.getInt("warehouseID"),
                        rs.getString("warehouseName")
                ));
            }

        } catch (SQLException e) {
            System.err.println("DeliveryService.loadPendingDeliveries error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Confirms a delivery and adds numOfItems to the warehouse stock.
     * CHANGED: Updated table names to unified schema.
     */
    /**
     * Confirms dispatch of a customer delivery (marks as 'Delivered' and updates the parent order).
     * Stock was already deducted when the order was placed — no stock adjustment here.
     */
    public ConfirmResult confirmDelivery(int deliveryId, int numItems, int warehouseId) {
        String updateDelivery = "UPDATE delivery_deliveries SET orderStatus = 'Delivered' WHERE deliveryID = ?";
        String updateOrder    = "UPDATE orders SET status = 'Delivered' WHERE orderID = " +
                                "(SELECT orderID FROM delivery_deliveries WHERE deliveryID = ?)";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(updateDelivery)) {
                ps.setInt(1, deliveryId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(updateOrder)) {
                ps.setInt(1, deliveryId);
                ps.executeUpdate();
            }

            conn.commit();
            return new ConfirmResult(true, warehouseId, -1, numItems);

        } catch (SQLException e) {
            System.err.println("DeliveryService.confirmDelivery error: " + e.getMessage());
            return new ConfirmResult(false, -1, -1, 0);
        }
    }

    public boolean rejectDelivery(int deliveryId) {
        // CHANGED: Delivery → delivery_deliveries
        String sql = "UPDATE delivery_deliveries SET orderStatus = 'Rejected' WHERE deliveryID = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deliveryId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("DeliveryService.rejectDelivery error: " + e.getMessage());
            return false;
        }
    }

    // ── ConfirmResult ──
    public static class ConfirmResult {
        private final boolean success;
        private final int warehouseId;
        private final int productId;
        private final int quantityAdded;

        public ConfirmResult(boolean success, int warehouseId, int productId, int quantityAdded) {
            this.success = success;
            this.warehouseId = warehouseId;
            this.productId = productId;
            this.quantityAdded = quantityAdded;
        }

        public boolean isSuccess()        { return success; }
        public int     getWarehouseId()   { return warehouseId; }
        public int     getProductId()     { return productId; }
        public int     getQuantityAdded() { return quantityAdded; }
    }

    // ── DeliveryRow ──
    public static class DeliveryRow {
        private final int deliveryIdInt;
        private final int warehouseIdInt;
        private final int numItemsInt;

        private final StringProperty deliveryId    = new SimpleStringProperty();
        private final StringProperty orderId       = new SimpleStringProperty();
        private final StringProperty address       = new SimpleStringProperty();
        private final StringProperty numItems      = new SimpleStringProperty();
        private final StringProperty driver        = new SimpleStringProperty();
        private final StringProperty orderDate     = new SimpleStringProperty();
        private final StringProperty warehouseName = new SimpleStringProperty();

        public DeliveryRow(int deliveryId, int orderId, String address,
                           int numItems, String driver, String orderDate,
                           int warehouseId, String warehouseName) {
            this.deliveryIdInt  = deliveryId;
            this.warehouseIdInt = warehouseId;
            this.numItemsInt    = numItems;

            this.deliveryId.set(String.valueOf(deliveryId));
            this.orderId.set(String.valueOf(orderId));
            this.address.set(address);
            this.numItems.set(String.valueOf(numItems));
            this.driver.set(driver);
            this.orderDate.set(orderDate);
            this.warehouseName.set(warehouseName);
        }

        public int getDeliveryIdInt()  { return deliveryIdInt; }
        public int getWarehouseIdInt() { return warehouseIdInt; }
        public int getNumItemsInt()    { return numItemsInt; }

        public StringProperty deliveryIdProperty()    { return deliveryId; }
        public StringProperty orderIdProperty()       { return orderId; }
        public StringProperty addressProperty()       { return address; }
        public StringProperty numItemsProperty()      { return numItems; }
        public StringProperty driverProperty()        { return driver; }
        public StringProperty orderDateProperty()     { return orderDate; }
        public StringProperty warehouseNameProperty() { return warehouseName; }
    }

    /**
     * Inserts 3 fresh dummy pending deliveries for demo purposes.
     * CHANGED: Delivery → delivery_deliveries
     */
    public boolean insertDemoDeliveries() {
        String sql = "INSERT INTO delivery_deliveries (orderID, customerAddress, orderStatus, " +
                     "orderDate, numOfItems, driverID, warehouseID) VALUES (?,?,?,?,?,?,?)";

        int[][] demos = {
            {1, 1, 3},
            {2, 2, 5},
            {3, 3, 2}
        };
        String[] addresses = {
            "123 High Street, London",
            "45 Baker Street, Manchester",
            "78 Park Lane, Birmingham"
        };
        String today = java.time.LocalDate.now().toString();

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < demos.length; i++) {
                stmt.setInt(1, demos[i][0]);
                stmt.setString(2, addresses[i]);
                stmt.setString(3, "Pending");
                stmt.setString(4, today);
                stmt.setInt(5, demos[i][2]);
                stmt.setInt(6, i + 1);
                stmt.setInt(7, demos[i][1]);
                stmt.addBatch();
            }
            stmt.executeBatch();
            return true;

        } catch (SQLException e) {
            System.err.println("DeliveryService.insertDemoDeliveries error: " + e.getMessage());
            return false;
        }
    }
}