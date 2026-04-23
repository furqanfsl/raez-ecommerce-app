package com.raez.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the unified {@code products} table.
 * Categories and images are loaded separately by ProductDAO
 * and attached here for convenience.
 */
public class Product {

    public int              productID;
    public String           sku;
    public String           name;
    public String           description;
    public double           price;
    public double           unitCost;
    public Integer          categoryID;
    public String           status;   // active | inactive | discontinued
    public String           collection; // e.g. Apex Automata, Sentinel Force, NovaMind, TerraCore
    public String           createdAt;
    public String           updatedAt;

    // Joined from product_categories + categories
    public List<Category>      categories = new ArrayList<>();

    // Joined from product_images
    public List<ProductImage>  images     = new ArrayList<>();

    // Convenience: total stock across all warehouses (from warehouse_inventory)
    public int stock = 0;

    // Computed from reviews_reviews — populated by ProductService
    public double avgRating  = 0.0;
    public int    reviewCount = 0;

    public Product() {}

    public Product(String sku, String name, String description,
                   double price, double unitCost, String status, Integer categoryID) {
        this.sku         = sku;
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.unitCost    = unitCost;
        this.status      = status;
        this.categoryID  = categoryID;
    }

    /** Returns primary image URL, or null if none */
    public String getPrimaryImage() {
        return images.stream()
            .filter(i -> i.isPrimary != 0)
            .map(i -> i.imageURL)
            .findFirst()
            .orElse(images.isEmpty() ? null : images.get(0).imageURL);
    }

    /** Returns category names as a comma-separated string */
    public String getCategoryNames() {
        return categories.stream()
            .map(c -> c.categoryName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("Uncategorised");
    }

    @Override
    public String toString() {
        return name + " (" + String.format("%.2f", price) + ")";
    }
}
