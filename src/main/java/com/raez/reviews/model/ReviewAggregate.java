package com.raez.reviews.model;

public class ReviewAggregate {
    private final double averageRating;
    private final int reviewCount;

    public ReviewAggregate(double averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }
}
