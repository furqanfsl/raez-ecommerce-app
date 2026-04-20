package com.raez.reviews.model;

public class AdminUser {
    private final int userId;
    private final String username;
    private final String displayName;
    private final boolean active;

    public AdminUser(int userId, String username, String displayName, boolean active) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }
}
