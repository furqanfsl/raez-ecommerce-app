package com.reaz.dao;

import com.reaz.db.DBConnection;
import com.reaz.model.ProductImage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL operations for the 'product_image' table.
 */
public class ImageDAO {

    private final Connection conn = DBConnection.getInstance().getConnection();

    /** Get all images for a product */
    public List<ProductImage> getByProduct(int productId) {
        List<ProductImage> list = new ArrayList<>();
        String sql = "SELECT * FROM product_image WHERE product_id = ? ORDER BY is_primary DESC";
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
        String sql = "INSERT INTO product_image (product_id, image_path, is_primary) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, img.productId);
            ps.setString(2, img.imagePath);
            ps.setInt(3, img.isPrimary ? 1 : 0);
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
        String sql = "DELETE FROM product_image WHERE product_id = ?";
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
                "UPDATE product_image SET is_primary = 0 WHERE product_id = ?");
             PreparedStatement ps2 = conn.prepareStatement(
                "UPDATE product_image SET is_primary = 1 WHERE id = ?")) {
            ps1.setInt(1, productId);
            ps1.executeUpdate();
            ps2.setInt(1, imageId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("ImageDAO.setPrimary: " + e.getMessage());
        }
    }

    private ProductImage map(ResultSet rs) throws SQLException {
        ProductImage img  = new ProductImage();
        img.id            = rs.getInt("id");
        img.productId     = rs.getInt("product_id");
        img.imagePath     = rs.getString("image_path");
        img.isPrimary     = rs.getInt("is_primary") == 1;
        img.uploadedAt    = rs.getString("uploaded_at");
        return img;
    }
}
