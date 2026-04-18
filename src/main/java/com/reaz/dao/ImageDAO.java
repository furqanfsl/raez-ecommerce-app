package com.reaz.dao;

import com.reaz.db.DBConnection;
import com.reaz.model.ProductImage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL operations for the {@code product_images} table.
 */
public class ImageDAO {

    private final Connection conn = DBConnection.getInstance().getConnection();

    /** Get all images for a product */
    public List<ProductImage> getByProduct(int productId) {
        List<ProductImage> list = new ArrayList<>();
        String sql = "SELECT * FROM product_images WHERE productID = ? ORDER BY isPrimary DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("ImageDAO.getByProduct: " + e.getMessage());
        }
        return list;
    }

    /** Insert a new image, returns generated id */
    public int insert(ProductImage img) {
        String sql =
            "INSERT INTO product_images (productID, imageURL, isPrimary) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, img.productID);
            ps.setString(2, img.imageURL);
            ps.setInt(3, img.isPrimary);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("ImageDAO.insert: " + e.getMessage());
        }
        return -1;
    }

    /** Delete all images for a product */
    public void deleteAllForProduct(int productId) {
        String sql = "DELETE FROM product_images WHERE productID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("ImageDAO.deleteAllForProduct: " + e.getMessage());
        }
    }

    /** Set a specific image as primary */
    public void setPrimary(int productId, int imageId) {
        try (PreparedStatement ps1 = conn.prepareStatement(
                "UPDATE product_images SET isPrimary = 0 WHERE productID = ?");
             PreparedStatement ps2 = conn.prepareStatement(
                "UPDATE product_images SET isPrimary = 1 WHERE imageID = ?")) {
            ps1.setInt(1, productId);
            ps1.executeUpdate();
            ps2.setInt(1, imageId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("ImageDAO.setPrimary: " + e.getMessage());
        }
    }

    private ProductImage map(ResultSet rs) throws SQLException {
        ProductImage img = new ProductImage();
        img.imageID    = rs.getInt("imageID");
        img.productID  = rs.getInt("productID");
        img.imageURL   = rs.getString("imageURL");
        img.isPrimary  = rs.getInt("isPrimary");
        img.uploadedAt = rs.getString("uploadedAt");
        return img;
    }
}
