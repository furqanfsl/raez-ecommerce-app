package com.reaz.dao;

import com.reaz.db.DBConnection;

import java.sql.*;

/**
 * Handles stock levels in inventory_record.
 * Uses a single default warehouse for the product component.
 */
public class InventoryDAO {

    private static final String DEFAULT_WAREHOUSE = "Main Warehouse";
    private final Connection conn = DBConnection.getInstance().getConnection();

    /** Get or create the default warehouse, returns its id */
    public int getOrCreateDefaultWarehouse() {
        // Try to find existing
        String find = "SELECT id FROM warehouse WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setString(1, DEFAULT_WAREHOUSE);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("InventoryDAO.findWarehouse: " + e.getMessage());
        }

        // Create it
        String insert = "INSERT INTO warehouse (name, location, status) VALUES (?, 'Main', 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(insert,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, DEFAULT_WAREHOUSE);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("InventoryDAO.createWarehouse: " + e.getMessage());
        }
        return -1;
    }

    /** Get stock quantity for a product (sum across all warehouses) */
    public int getStock(int productId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM inventory_record WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("InventoryDAO.getStock: " + e.getMessage());
        }
        return 0;
    }

    /** Set stock for a product in the default warehouse (insert or update) */
    public void setStock(int productId, int quantity) {
        int warehouseId = getOrCreateDefaultWarehouse();
        if (warehouseId < 0) return;

        String sql = """
            INSERT INTO inventory_record (product_id, warehouse_id, quantity, min_stock)
            VALUES (?, ?, ?, 0)
            ON CONFLICT(product_id, warehouse_id)
            DO UPDATE SET quantity = excluded.quantity,
                          last_updated = CURRENT_TIMESTAMP
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
            ps.setInt(3, Math.max(0, quantity));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("InventoryDAO.setStock: " + e.getMessage());
        }
    }

    /** Delete stock record when product is deleted */
    public void deleteForProduct(int productId) {
        String sql = "DELETE FROM inventory_record WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("InventoryDAO.deleteForProduct: " + e.getMessage());
        }
    }
}