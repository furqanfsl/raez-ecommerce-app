package com.raez.reviews.model;

public enum ReviewStatus {
    ACTIVE,
    FLAGGED,
    REMOVED,
    CUSTOMER_DELETED;

    public boolean countsTowardsRating() {
        return this == ACTIVE || this == FLAGGED;
    }

    public boolean visibleToCustomers() {
        return this == ACTIVE || this == FLAGGED;
    }
}
