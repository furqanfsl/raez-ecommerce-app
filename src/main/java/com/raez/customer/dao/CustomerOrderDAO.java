package com.raez.customer.dao;

import com.raez.customer.model.CustomerOrder;
import com.raez.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerOrderDAO {

    // ── ORDERS BY USER ID ──────────────────────────────────────────────────
    public List<CustomerOrder> getOrdersByUserId(int userId) throws SQLException {
        String sql =
            "SELECT o.orderID, o.orderDate, o.status, o.totalAmount, " +
            "       p.name AS productName " +
            "FROM orders o " +
            "JOIN customers c ON c.customerID = o.customerID " +
            "LEFT JOIN order_items oi ON oi.orderID = o.orderID " +
            "LEFT JOIN products p ON p.productID = oi.productID " +
            "WHERE c.userID = ? " +
            "ORDER BY o.orderDate DESC";

        List<CustomerOrder> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CustomerOrder(
                    rs.getInt("orderID"),
                    rs.getString("orderDate"),
                    rs.getString("status"),
                    rs.getDouble("totalAmount"),
                    rs.getString("productName")
                ));
            }
        }
        return list;
    }

    // ── TOTAL SPENT BY USER ID ─────────────────────────────────────────────
    public double getTotalSpentByUserId(int userId) throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM orders o " +
            "JOIN customers c ON c.customerID = o.customerID " +
            "WHERE c.userID = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }
}
