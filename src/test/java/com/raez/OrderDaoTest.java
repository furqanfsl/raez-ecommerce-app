package com.raez;

import com.raez.db.DBConnection;
import com.raez.model.CartManager;
import com.raez.orders.dao.OrderDAO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderDaoTest {

    private static final int CUSTOMER_ID = 6;   // Alice (seed)
    private static final int PRODUCT_ID  = 59;  // Aria Home Assistant — stocked in WH 1 + 2

    private final OrderDAO dao = new OrderDAO();

    @BeforeAll
    static void boot() {
        TestDb.init();
    }

    @Test
    void placeOrder_happyPath_persistsAndDecrementsStock() throws Exception {
        int stockBefore = totalStock(PRODUCT_ID);
        Map<Integer, CartManager.CartItem> cart = new HashMap<>();
        cart.put(PRODUCT_ID, new CartManager.CartItem(PRODUCT_ID, "Aria Home Assistant", 299.99, 2));

        int orderId = dao.createOrder(CUSTOMER_ID, cart, "1 Test Lane, London");

        assertTrue(orderId > 0, "createOrder should return a generated orderID");
        assertEquals(stockBefore - 2, totalStock(PRODUCT_ID), "stock should decrement by ordered qty");
        assertTrue(orderExists(orderId), "orders row should be persisted");
    }

    @Test
    void placeOrder_insufficientStock_throwsAndDoesNotMutate() throws Exception {
        int stockBefore = totalStock(PRODUCT_ID);
        long ordersBefore = countOrders();
        Map<Integer, CartManager.CartItem> cart = new HashMap<>();
        cart.put(PRODUCT_ID, new CartManager.CartItem(PRODUCT_ID, "Aria Home Assistant", 299.99, 999_999));

        Exception ex = assertThrows(Exception.class,
                () -> dao.createOrder(CUSTOMER_ID, cart, "nowhere"),
                "should reject orders that exceed available stock");
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient stock"),
                "exception message should explain the stock failure: " + ex.getMessage());

        assertEquals(stockBefore, totalStock(PRODUCT_ID), "stock must be unchanged after rollback");
        assertEquals(ordersBefore, countOrders(), "no order row should be left behind");
    }

    private int totalStock(int productId) throws Exception {
        Connection c = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(quantityOnHand),0) FROM warehouse_inventory WHERE productID = ?")) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private boolean orderExists(int orderId) throws Exception {
        Connection c = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM orders WHERE orderID = ?")) {
            ps.setInt(1, orderId);
            return ps.executeQuery().next();
        }
    }

    private long countOrders() throws Exception {
        Connection c = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
