package com.raez.reviews.model;

import java.time.LocalDateTime;

public class Review {
    private final int reviewId;
    private final int productId;
    private final int customerId;
    private final String productName;
    private final String customerName;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final ReviewStatus status;
    private final int helpfulCount;
    private final int unhelpfulCount;

    public Review(
            int reviewId,
            int productId,
            int customerId,
            String productName,
            String customerName,
            int rating,
            String comment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            ReviewStatus status,
            int helpfulCount,
            int unhelpfulCount) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.customerId = customerId;
        this.productName = productName;
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.helpfulCount = helpfulCount;
        this.unhelpfulCount = unhelpfulCount;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getProductId() {
        return productId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public int getUnhelpfulCount() {
        return unhelpfulCount;
    }

    public int getHelpfulnessScore() {
        return helpfulCount - unhelpfulCount;
    }
}
