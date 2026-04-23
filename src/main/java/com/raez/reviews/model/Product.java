package com.raez.reviews.model;

public class Product {
    private final int productId;
    private final String name;
    private final String category;
    private final boolean active;
    private final double averageRating;
    private final int reviewCount;

    public Product(int productId, String name, String category, boolean active, double averageRating, int reviewCount) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.active = active;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}
