package com.reaz.model;

/**
 * Matches the unified {@code product_images} table.
 */
public class ProductImage {

    public int     imageID;
    public int     productID;
    public String  imageURL;
    /** 1 = primary image, 0 = not primary */
    public int     isPrimary;
    public String  uploadedAt;

    public ProductImage() {}

    public ProductImage(int productID, String imageURL, int isPrimary) {
        this.productID = productID;
        this.imageURL  = imageURL;
        this.isPrimary = isPrimary;
    }
}
