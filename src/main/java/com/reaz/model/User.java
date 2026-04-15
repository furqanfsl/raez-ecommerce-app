package com.reaz.model;

public class User {
    public int    id;
    public String name;
    public String email;
    public String role;    // ADMIN or CUSTOMER
    public String status;  // ACTIVE or INACTIVE

    public User() {}

    public User(int id, String name, String email, String role, String status) {
        this.id     = id;
        this.name   = name;
        this.email  = email;
        this.role   = role;
        this.status = status;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}