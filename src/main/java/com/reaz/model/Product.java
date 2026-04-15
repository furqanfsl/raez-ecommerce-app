package com.reaz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the 'product' table.
 * Categories and images are loaded separately by ProductDAO
 * and attached here for convenience.
 */
public class Product {

    public int              id;
    public String           sku;
    public String           name;
    public String           description;
    public double           price;
    public String           status;   // ACTIVE | INACTIVE
    public String           createdAt;
    public String           updatedAt;

    // Joined from product_categories + category
    public List<Category>      categories = new ArrayList<>();

    // Joined from product_image
    public List<ProductImage>  images     = new ArrayList<>();

    // Convenience: total stock across all warehouses (from inventory_record)
    public int stock = 0;

    public Product() {}

    public Product(String sku, String name, String description,
                   double price, String status) {
        this.sku         = sku;
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.status      = status;
    }

    /** Returns primary image path, or null if none */
    public String getPrimaryImage() {
        return images.stream()
            .filter(i -> i.isPrimary)
            .map(i -> i.imagePath)
            .findFirst()
            .orElse(images.isEmpty() ? null : images.get(0).imagePath);
    }

    /** Returns category names as a comma-separated string */
    public String getCategoryNames() {
        return categories.stream()
            .map(c -> c.name)
            .reduce((a, b) -> a + ", " + b)
            .orElse("Uncategorised");
    }

    @Override
    public String toString() {
        return name + " (£" + String.format("%.2f", price) + ")";
    }
}