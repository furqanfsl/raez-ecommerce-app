package com.raez.reviews.model;

public class Customer {
    private final int customerId;
    private final String fullName;
    private final String email;
    private final boolean active;

    public Customer(int customerId, String fullName, String email, boolean active) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }
}
