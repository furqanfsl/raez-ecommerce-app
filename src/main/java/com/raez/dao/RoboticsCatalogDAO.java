package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoboticsCatalogDAO {

    public record CollectionInfo(int collectionID, String name, String slug, String description) {}
    public record ProductWithCategory(Product product, String categoryName) {}

    private Connection conn() { return DBConnection.getInstance().getConnection(); }

    public CollectionInfo getCollectionBySlug(String slug) {
        String sql = """
            SELECT collectionID, name, slug, description
            FROM product_collections
            WHERE slug = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, slug);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new CollectionInfo(
                    rs.getInt("collectionID"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getString("description")
                );
            }
        } catch (SQLException e) {
            System.err.println("RoboticsCatalogDAO.getCollectionBySlug: " + e.getMessage());
        }
        return null;
    }

    public List<ProductWithCategory> getProductsByCollection(int collectionID) {
        String sql = """
            SELECT p.*,
                (SELECT c2.categoryName
                 FROM product_categories pc2
                 JOIN categories c2 ON c2.categoryID = pc2.categoryID
                 WHERE pc2.productID = p.productID
                 ORDER BY CASE c2.categoryName
                     WHEN 'Main Robot' THEN 1
                     WHEN 'Mini Robot' THEN 2
                     WHEN 'Accessory' THEN 3
                     WHEN 'Service' THEN 4
                     ELSE 5 END
                 LIMIT 1) AS categoryName
            FROM products p
            WHERE p.collectionID = ?
              AND LOWER(p.status) = 'active'
            ORDER BY
                COALESCE((
                    SELECT CASE c2.categoryName
                        WHEN 'Main Robot' THEN 1
                        WHEN 'Mini Robot' THEN 2
                        WHEN 'Accessory' THEN 3
                        WHEN 'Service' THEN 4
                        ELSE 5 END
                    FROM product_categories pc2
                    JOIN categories c2 ON c2.categoryID = pc2.categoryID
                    WHERE pc2.productID = p.productID
                    ORDER BY CASE c2.categoryName WHEN 'Main Robot' THEN 1 ELSE 2 END
                    LIMIT 1
                ), 5),
                p.name
            """;
        List<ProductWithCategory> rows = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, collectionID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new ProductWithCategory(mapProduct(rs), rs.getString("categoryName")));
            }
        } catch (SQLException e) {
            System.err.println("RoboticsCatalogDAO.getProductsByCollection: " + e.getMessage());
        }
        return rows;
    }

    public ProductWithCategory getProductWithCategoryById(int productID) {
        String sql = """
            SELECT p.*,
                (SELECT c2.categoryName
                 FROM product_categories pc2
                 JOIN categories c2 ON c2.categoryID = pc2.categoryID
                 WHERE pc2.productID = p.productID
                 ORDER BY CASE c2.categoryName
                     WHEN 'Main Robot' THEN 1
                     WHEN 'Mini Robot' THEN 2
                     WHEN 'Accessory'  THEN 3
                     WHEN 'Service'    THEN 4
                     ELSE 5 END
                 LIMIT 1) AS categoryName
            FROM products p
            WHERE p.productID = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ProductWithCategory(mapProduct(rs), rs.getString("categoryName"));
            }
        } catch (SQLException e) {
            System.err.println("RoboticsCatalogDAO.getProductWithCategoryById: " + e.getMessage());
        }
        return null;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.productID = rs.getInt("productID");
        p.sku = rs.getString("sku");
        p.name = rs.getString("name");
        p.description = rs.getString("description");
        p.price = rs.getDouble("price");
        p.unitCost = rs.getDouble("unitCost");
        p.status = rs.getString("status");
        int cid = rs.getInt("categoryID");
        p.categoryID = rs.wasNull() ? null : cid;
        p.collection = rs.getString("collection");
        p.imagePath = null; // legacy column dropped in D5.7
        try { p.imageUrl = rs.getString("imageUrl"); } catch (SQLException ignore) { p.imageUrl = null; }
        try { p.imagePublicId = rs.getString("imagePublicId"); } catch (SQLException ignore) { p.imagePublicId = null; }
        p.createdAt = rs.getString("createdAt");
        p.updatedAt = rs.getString("updatedAt");
        return p;
    }
}
