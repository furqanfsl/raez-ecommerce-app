package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All SQL operations for the {@code categories} table.
 */
public class CategoryDAO {
    private static final Logger log = LoggerFactory.getLogger(CategoryDAO.class);


    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    /** Get all active categories */
    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE isActive = 1 ORDER BY categoryName";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    /** Get categories for a specific product */
    public List<Category> getByProduct(int productId) {
        List<Category> list = new ArrayList<>();
        String sql = """
            SELECT c.* FROM categories c
            JOIN product_categories pc ON pc.categoryID = c.categoryID
            WHERE pc.productID = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.getByProduct: " + e.getMessage());
        }
        return list;
    }

    /** Insert a new category, returns generated id */
    public int insert(Category c) {
        String sql =
            "INSERT INTO categories (categoryName, description, isActive) VALUES (?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.categoryName);
            ps.setString(2, c.description);
            ps.setInt(3, c.isActive);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.insert: " + e.getMessage());
        }
        return -1;
    }

    /** Find category by name, returns null if not found */
    public Category findByName(String name) {
        String sql = "SELECT * FROM categories WHERE categoryName = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.findByName: " + e.getMessage());
        }
        return null;
    }

    /** Link a product to a category */
    public void linkProductCategory(int productId, int categoryId) {
        String sql = "INSERT OR IGNORE INTO product_categories (productID, categoryID) VALUES (?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, categoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.linkProductCategory: " + e.getMessage());
        }
    }

    /** Remove all category links for a product */
    public void unlinkAllForProduct(int productId) {
        String sql = "DELETE FROM product_categories WHERE productID = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("{}", "CategoryDAO.unlinkAllForProduct: " + e.getMessage());
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c   = new Category();
        c.categoryID   = rs.getInt("categoryID");
        c.categoryName = rs.getString("categoryName");
        c.description  = rs.getString("description");
        int pid = rs.getInt("parentID");
        c.parentID     = rs.wasNull() ? 0 : pid;
        c.isActive     = rs.getInt("isActive");
        return c;
    }
}
