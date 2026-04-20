package com.raez.reviews.model;

public enum ReviewSortOption {
    NEWEST("Newest"),
    RATING_HIGH("Highest Rating"),
    HELPFULNESS("Helpfulness");

    private final String label;

    ReviewSortOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
