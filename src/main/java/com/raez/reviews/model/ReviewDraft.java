package com.raez.reviews.model;

public class ReviewDraft {
    private final int rating;
    private final String comment;

    public ReviewDraft(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
