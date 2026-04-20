package com.raez.reviews.model;

public class UserSession {
    public enum Role {
        CUSTOMER,
        ADMIN
    }

    private final Role role;
    private final Integer customerId;
    private final Integer adminUserId;
    private final String displayName;

    private UserSession(Role role, Integer customerId, Integer adminUserId, String displayName) {
        this.role = role;
        this.customerId = customerId;
        this.adminUserId = adminUserId;
        this.displayName = displayName;
    }

    public static UserSession customer(Customer customer) {
        return new UserSession(Role.CUSTOMER, customer.getCustomerId(), null, customer.getFullName());
    }

    public static UserSession admin(AdminUser adminUser) {
        return new UserSession(Role.ADMIN, null, adminUser.getUserId(), adminUser.getDisplayName());
    }

    public Role getRole() {
        return role;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getAdminUserId() {
        return adminUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
