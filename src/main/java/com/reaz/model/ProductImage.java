package com.reaz.model;

/**
 * Matches the 'product_image' table.
 */
public class ProductImage {

    public int     id;
    public int     productId;
    public String  imagePath;
    public boolean isPrimary;
    public String  uploadedAt;

    public ProductImage() {}

    public ProductImage(int productId, String imagePath, boolean isPrimary) {
        this.productId = productId;
        this.imagePath = imagePath;
        this.isPrimary = isPrimary;
    }
}