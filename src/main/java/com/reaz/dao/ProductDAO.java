package com.reaz.dao;

import com.reaz.db.DBConnection;
import com.reaz.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL operations for the 'product' table.
 * Does NOT load categories/images — ProductService handles that.
 */
public class ProductDAO {

    private final Connection conn = DBConnection.getInstance().getConnection();

    /** Get all products ordered by newest first */
    public List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product ORDER BY updated_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    /** Get only active products */
    public List<Product> getActive() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE status = 'ACTIVE' ORDER BY name";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.getActive: " + e.getMessage());
        }
        return list;
    }

    /** Get product by ID */
    public Product getById(int id) {
        String sql = "SELECT * FROM product WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("ProductDAO.getById: " + e.getMessage());
        }
        return null;
    }

    /** Search products by name or description */
    public List<Product> search(String query) {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT * FROM product
            WHERE (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)
            ORDER BY name
            """;
        String like = "%" + query.toLowerCase() + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.search: " + e.getMessage());
        }
        return list;
    }

    /** Filter products by price range */
    public List<Product> filterByPrice(double min, double max) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE price >= ? AND price <= ? ORDER BY price";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, min);
            ps.setDouble(2, max);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.filterByPrice: " + e.getMessage());
        }
        return list;
    }

    /** Filter products by status */
    public List<Product> filterByStatus(String status) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE status = ? ORDER BY name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.filterByStatus: " + e.getMessage());
        }
        return list;
    }

    /** Insert a new product, returns generated id */
    public int insert(Product p) {
        String sql = """
            INSERT INTO product (sku, name, description, price, status)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.sku);
            ps.setString(2, p.name);
            ps.setString(3, p.description);
            ps.setDouble(4, p.price);
            ps.setString(5, p.status != null ? p.status.toUpperCase() : "ACTIVE");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                p.id = id;
                return id;
            }
        } catch (SQLException e) {
            System.err.println("ProductDAO.insert: " + e.getMessage());
        }
        return -1;
    }

    /** Update an existing product */
    public boolean update(Product p) {
        String sql = """
            UPDATE product
            SET name=?, description=?, price=?, status=?,
                updated_at=CURRENT_TIMESTAMP
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.name);
            ps.setString(2, p.description);
            ps.setDouble(3, p.price);
            ps.setString(4, p.status.toUpperCase());
            ps.setInt(5, p.id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProductDAO.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a product and ALL child rows that reference it.
     * Order matters — deepest dependencies deleted first.
     */
    public boolean delete(int id) {
        try {
            conn.setAutoCommit(false);

            // 1. Stock movements (depend on inventory_record)
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM stock_movement WHERE inventory_id IN " +
                    "(SELECT id FROM inventory_record WHERE product_id = ?)")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 2. Inventory records
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM inventory_record WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 3. Product images
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM product_image WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 4. Product categories
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM product_categories WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 5. Product validation logs
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM product_validation WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 6. Reviews
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM review WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 7. Alerts
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM alert WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 8. Financial anomalies
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM financial_anomalies WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 9. Refunds linked to this product
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM refund WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 10. Order items
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM order_item WHERE product_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // 11. Finally delete the product itself
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM product WHERE id = ?")) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }

        } catch (SQLException e) {
            System.err.println("ProductDAO.delete: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ex) {
                System.err.println("ProductDAO.delete rollback failed: " + ex.getMessage());
            }
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {
                System.err.println("ProductDAO.delete autoCommit reset failed: " + e.getMessage());
            }
        }
    }

    /** Toggle product status ACTIVE ↔ INACTIVE */
    public boolean setStatus(int id, String status) {
        String sql = "UPDATE product SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProductDAO.setStatus: " + e.getMessage());
            return false;
        }
    }

    /** Count total products */
    public int count() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM product")) {
            return rs.getInt(1);
        } catch (SQLException e) { return 0; }
    }

    /** Check if any products exist */
    public boolean isEmpty() {
        return count() == 0;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p     = new Product();
        p.id          = rs.getInt("id");
        p.sku         = rs.getString("sku");
        p.name        = rs.getString("name");
        p.description = rs.getString("description");
        p.price       = rs.getDouble("price");
        p.status      = rs.getString("status");
        p.createdAt   = rs.getString("created_at");
        p.updatedAt   = rs.getString("updated_at");
        return p;
    }
}