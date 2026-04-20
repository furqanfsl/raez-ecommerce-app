package com.reaz.orders.dao;

import com.reaz.db.DBConnection;
import com.reaz.model.CartManager;
import com.reaz.orders.model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDAO {

    public List<Order> getAllOrders() {
        String sql = """
            SELECT o.orderID, COALESCE(c.name,'Unknown') AS customerName,
                   o.orderDate, o.totalAmount, o.status,
                   COUNT(oi.orderItemID) AS itemCount
            FROM orders o
            LEFT JOIN customers c  ON c.customerID = o.customerID
            LEFT JOIN order_items oi ON oi.orderID = o.orderID
            GROUP BY o.orderID
            ORDER BY o.orderDate DESC
            """;
        return query(sql, (Object[]) null);
    }

    public List<Order> getOrdersByStatus(String status) {
        String sql = """
            SELECT o.orderID, COALESCE(c.name,'Unknown') AS customerName,
                   o.orderDate, o.totalAmount, o.status,
                   COUNT(oi.orderItemID) AS itemCount
            FROM orders o
            LEFT JOIN customers c  ON c.customerID = o.customerID
            LEFT JOIN order_items oi ON oi.orderID = o.orderID
            WHERE o.status = ?
            GROUP BY o.orderID
            ORDER BY o.orderDate DESC
            """;
        return query(sql, status);
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE orders SET status = ? WHERE orderID = ?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("OrderDAO.updateOrderStatus: " + e.getMessage());
            return false;
        }
    }

    /** Returns customerID for a given userID, or -1 if not found. */
    public int getCustomerIdByUserId(int userId) {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT customerID FROM customers WHERE userID = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("customerID") : -1;
        } catch (SQLException e) {
            System.err.println("OrderDAO.getCustomerIdByUserId: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Creates an order:
     *   1. Validates stock for every cart item
     *   2. Inserts orders + order_items
     *   3. Deducts stock from warehouse_inventory (highest-stock warehouse first)
     *   4. Creates a delivery_deliveries record (orderStatus = 'Pending')
     * Returns new orderID, throws on any failure.
     */
    public int createOrder(int customerId, Map<Integer, CartManager.CartItem> cartItems,
                           String deliveryAddress) throws Exception {
        Connection conn = DBConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            for (Map.Entry<Integer, CartManager.CartItem> e : cartItems.entrySet()) {
                int stock = getStockTx(conn, e.getKey());
                if (stock < e.getValue().quantity) {
                    throw new Exception("Insufficient stock for: " + e.getValue().productName
                            + " (available: " + stock + ", requested: " + e.getValue().quantity + ")");
                }
            }

            double total = cartItems.values().stream()
                    .mapToDouble(i -> i.price * i.quantity).sum();

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (customerID, totalAmount, status) VALUES (?, ?, 'Processing')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setDouble(2, total);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new Exception("Order insert failed.");
                orderId = keys.getInt(1);
            }

            for (Map.Entry<Integer, CartManager.CartItem> e : cartItems.entrySet()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO order_items (orderID, productID, quantity, unitPrice) VALUES (?,?,?,?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, e.getKey());
                    ps.setInt(3, e.getValue().quantity);
                    ps.setDouble(4, e.getValue().price);
                    ps.executeUpdate();
                }
                deductStockTx(conn, e.getKey(), e.getValue().quantity);
            }

            int totalItems = cartItems.values().stream().mapToInt(i -> i.quantity).sum();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO delivery_deliveries (orderID, customerAddress, orderStatus, orderDate, numOfItems)" +
                    " VALUES (?, ?, 'Pending', date('now'), ?)")) {
                ps.setInt(1, orderId);
                ps.setString(2, deliveryAddress != null ? deliveryAddress : "");
                ps.setInt(3, totalItems);
                ps.executeUpdate();
            }

            conn.commit();
            return orderId;
        } catch (Exception ex) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw ex;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    /** Marks delivery as 'Delivered' and updates the parent order to 'Delivered'. */
    public boolean markDelivered(int deliveryId) {
        Connection conn = DBConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT orderID FROM delivery_deliveries WHERE deliveryID = ?")) {
                ps.setInt(1, deliveryId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { conn.rollback(); return false; }
                orderId = rs.getInt("orderID");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE delivery_deliveries SET orderStatus='Delivered' WHERE deliveryID=?")) {
                ps.setInt(1, deliveryId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET status='Delivered' WHERE orderID=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            System.err.println("OrderDAO.markDelivered: " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    // ── private helpers ────────────────────────────────────────────────────

    private List<Order> query(String sql, Object... params) {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                    rs.getInt("orderID"),
                    rs.getString("customerName"),
                    rs.getString("orderDate"),
                    rs.getDouble("totalAmount"),
                    rs.getString("status"),
                    rs.getInt("itemCount")
                ));
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO.query: " + e.getMessage());
        }
        return list;
    }

    private int getStockTx(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(quantityOnHand),0) FROM warehouse_inventory WHERE productID=?")) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void deductStockTx(Connection conn, int productId, int qty) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE warehouse_inventory SET quantityOnHand = quantityOnHand - ? " +
                "WHERE inventoryID = (" +
                "  SELECT inventoryID FROM warehouse_inventory " +
                "  WHERE productID=? AND quantityOnHand >= ? " +
                "  ORDER BY quantityOnHand DESC LIMIT 1)")) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Failed to deduct stock for productID=" + productId);
        }
    }
}
