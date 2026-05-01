package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FavouritesDAO {
    private static final Logger log = LoggerFactory.getLogger(FavouritesDAO.class);


    public int getCustomerIdByUserId(int userID) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT customerID FROM customers WHERE userID = ?")) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("{}", "FavouritesDAO.getCustomerIdByUserId: " + e.getMessage());
        }
        return -1;
    }

    /** Lightweight product load — enough for the favourites popup and heart state. */
    public List<Product> loadFavouriteProducts(int customerID) {
        String sql =
            "SELECT p.productID, p.name, p.price, p.sku, p.status, p.description " +
            "FROM customer_favourites cf " +
            "JOIN products p ON p.productID = cf.productID " +
            "WHERE cf.customerID = ? ORDER BY cf.addedAt DESC";
        List<Product> out = new ArrayList<>();
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product p = new Product();
                p.productID   = rs.getInt("productID");
                p.name        = rs.getString("name");
                p.price       = rs.getDouble("price");
                p.sku         = rs.getString("sku");
                p.status      = rs.getString("status");
                p.description = rs.getString("description");
                out.add(p);
            }
        } catch (SQLException e) {
            log.error("{}", "FavouritesDAO.loadFavouriteProducts: " + e.getMessage());
        }
        return out;
    }

    public boolean add(int customerID, int productID) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT OR IGNORE INTO customer_favourites (customerID, productID) VALUES (?, ?)")) {
            ps.setInt(1, customerID);
            ps.setInt(2, productID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("{}", "FavouritesDAO.add: " + e.getMessage());
            return false;
        }
    }

    public boolean remove(int customerID, int productID) {
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM customer_favourites WHERE customerID = ? AND productID = ?")) {
            ps.setInt(1, customerID);
            ps.setInt(2, productID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("{}", "FavouritesDAO.remove: " + e.getMessage());
            return false;
        }
    }
}
