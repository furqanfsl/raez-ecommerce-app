package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL operations for the {@code products} table.
 * Does NOT load categories/images — ProductService handles that.
 */
public class ProductDAO {

    private Connection conn() { return DBConnection.getInstance().getConnection(); }

    /** Get all products ordered by newest first */
    public List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY updatedAt DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.getAll: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /** Get only active products */
    public List<Product> getActive() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE LOWER(status) = 'active' ORDER BY name";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ProductDAO.getActive: " + e.getMessage());
        }
        return list;
    }

    /** Get product by ID */
    public Product getById(int id) {
        String sql = "SELECT * FROM products WHERE productID = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
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
            SELECT * FROM products
            WHERE (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)
            ORDER BY name
            """;
        String like = "%" + query.toLowerCase() + "%";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
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
        String sql = "SELECT * FROM products WHERE price >= ? AND price <= ? ORDER BY price";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
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
        String sql = "SELECT * FROM products WHERE LOWER(status) = LOWER(?) ORDER BY name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
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
            INSERT INTO products (sku, name, description, price, unitCost, status, categoryID,
                imageUrl, imagePublicId, collection, collectionID)
            VALUES (?,?,?,?,?,?,?,?,?,?,
                (SELECT pc.collectionID FROM product_collections pc WHERE pc.name = ?))
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.sku);
            ps.setString(2, p.name);
            ps.setString(3, p.description);
            ps.setDouble(4, p.price);
            ps.setDouble(5, p.unitCost);
            ps.setString(6, p.status != null ? p.status.toLowerCase() : "active");
            if (p.categoryID != null) {
                ps.setInt(7, p.categoryID);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            if (p.imageUrl != null && !p.imageUrl.isBlank()) {
                ps.setString(8, p.imageUrl);
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            if (p.imagePublicId != null && !p.imagePublicId.isBlank()) {
                ps.setString(9, p.imagePublicId);
            } else {
                ps.setNull(9, Types.VARCHAR);
            }
            if (p.collection != null && !p.collection.isBlank()) {
                ps.setString(10, p.collection);
                ps.setString(11, p.collection);
            } else {
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
            }
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                p.productID = id;
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
            UPDATE products
            SET name=?, description=?, price=?, unitCost=?, categoryID=?, status=?,
                imageUrl=?, imagePublicId=?,
                collection=?,
                collectionID = COALESCE(
                    (SELECT pc.collectionID FROM product_collections pc WHERE pc.name = ?),
                    collectionID
                ),
                updatedAt=CURRENT_TIMESTAMP
            WHERE productID=?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.name);
            ps.setString(2, p.description);
            ps.setDouble(3, p.price);
            ps.setDouble(4, p.unitCost);
            if (p.categoryID != null) {
                ps.setInt(5, p.categoryID);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, p.status != null ? p.status.toLowerCase() : "active");
            if (p.imageUrl != null && !p.imageUrl.isBlank()) {
                ps.setString(7, p.imageUrl);
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            if (p.imagePublicId != null && !p.imagePublicId.isBlank()) {
                ps.setString(8, p.imagePublicId);
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            if (p.collection != null && !p.collection.isBlank()) {
                ps.setString(9, p.collection);
                ps.setString(10, p.collection);
            } else {
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
            }
            ps.setInt(11, p.productID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProductDAO.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a product and dependent rows that reference it (unified schema).
     * Order respects foreign keys before deleting from {@code products}.
     */
    public boolean delete(int id) {
        Connection conn = conn();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM finance_refunds WHERE productID = ? OR orderItemID IN "
                    + "(SELECT orderItemID FROM order_items WHERE productID = ?)")) {
                ps.setInt(1, id);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM order_items WHERE productID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM finance_alerts WHERE anomalyID IN "
                    + "(SELECT anomalyID FROM finance_anomalies WHERE affectedProductID = ?)")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM finance_anomalies WHERE affectedProductID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM finance_supplier_orders WHERE productID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM finance_supplier_products WHERE productID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM warehouse_stock_movements WHERE inventoryID IN "
                    + "(SELECT inventoryID FROM warehouse_inventory WHERE productID = ?)")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM warehouse_inventory WHERE productID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM warehouse_restock_requests WHERE productID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM products WHERE productID = ?")) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }

        } catch (SQLException e) {
            System.err.println("ProductDAO.delete: " + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("ProductDAO.delete rollback failed: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("ProductDAO.delete autoCommit reset failed: " + e.getMessage());
            }
        }
    }

    /** Toggle product status active ↔ inactive */
    public boolean setStatus(int id, String status) {
        String sql = "UPDATE products SET status=?, updatedAt=CURRENT_TIMESTAMP WHERE productID=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status != null ? status.toLowerCase() : "active");
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProductDAO.setStatus: " + e.getMessage());
            return false;
        }
    }

    /** Count total products */
    public int count() {
        try (PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    /** Check if any products exist */
    public boolean isEmpty() {
        return count() == 0;
    }

    /**
     * Returns a map of productID → [avgRating, reviewCount] populated from
     * reviews_reviews (status = ACTIVE). Single query for all products.
     */
    public java.util.Map<Integer, double[]> getRatingsMap() {
        java.util.Map<Integer, double[]> map = new java.util.HashMap<>();
        String sql = "SELECT productID, AVG(rating) AS avg, COUNT(*) AS cnt " +
                     "FROM reviews_reviews WHERE status = 'ACTIVE' GROUP BY productID";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt("productID"),
                        new double[]{rs.getDouble("avg"), rs.getInt("cnt")});
            }
        } catch (SQLException e) {
            System.err.println("ProductDAO.getRatingsMap: " + e.getMessage());
        }
        return map;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p     = new Product();
        p.productID   = rs.getInt("productID");
        p.sku         = rs.getString("sku");
        p.name        = rs.getString("name");
        p.description = rs.getString("description");
        p.price       = rs.getDouble("price");
        p.unitCost    = rs.getDouble("unitCost");
        int cid = rs.getInt("categoryID");
        if (rs.wasNull()) {
            p.categoryID = null;
        } else {
            p.categoryID = cid;
        }
        p.status      = rs.getString("status");
        p.imagePath = null; // legacy column dropped in D5.7
        try {
            p.imageUrl = rs.getString("imageUrl");
        } catch (SQLException ignore) {
            p.imageUrl = null;
        }
        try {
            p.imagePublicId = rs.getString("imagePublicId");
        } catch (SQLException ignore) {
            p.imagePublicId = null;
        }
        try {
            p.collection = rs.getString("collection");
        } catch (SQLException ignore) {
            // Column absent on legacy databases — migration will add it on next boot.
            p.collection = null;
        }
        p.createdAt   = rs.getString("createdAt");
        p.updatedAt   = rs.getString("updatedAt");
        return p;
    }
}
