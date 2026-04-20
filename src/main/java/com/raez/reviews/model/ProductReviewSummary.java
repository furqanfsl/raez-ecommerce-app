package com.raez.reviews.model;

import java.util.Arrays;

public class ProductReviewSummary {
    private final Product product;
    private final int[] ratingBuckets;

    public ProductReviewSummary(Product product, int[] ratingBuckets) {
        this.product = product;
        this.ratingBuckets = Arrays.copyOf(ratingBuckets, ratingBuckets.length);
    }

    public Product getProduct() {
        return product;
    }

    public int[] getRatingBuckets() {
        return Arrays.copyOf(ratingBuckets, ratingBuckets.length);
    }
}
