package com.raez.reviews.service;

import com.raez.reviews.dao.AdminDao;
import com.raez.reviews.dao.CustomerDao;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.AdminUser;
import com.raez.reviews.model.Customer;
import com.raez.reviews.model.UserSession;

public class AuthService {
    private final CustomerDao customerDao;
    private final AdminDao adminDao;

    public AuthService(CustomerDao customerDao, AdminDao adminDao) {
        this.customerDao = customerDao;
        this.adminDao = adminDao;
    }

    public UserSession loginCustomer(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Email and password are required.");
        }
        Customer customer = customerDao.authenticate(email.trim(), password)
                .orElseThrow(() -> new BusinessException("Customer login details are invalid."));
        if (!customer.isActive()) {
            throw new BusinessException("This customer account is inactive.");
        }
        return UserSession.customer(customer);
    }

    public UserSession loginAdmin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Username and password are required.");
        }
        AdminUser adminUser = adminDao.authenticate(username.trim(), password)
                .orElseThrow(() -> new BusinessException("Admin login details are invalid."));
        if (!adminUser.isActive()) {
            throw new BusinessException("This admin account is inactive.");
        }
        return UserSession.admin(adminUser);
    }

    public UserSession loginWithFallback(String preferredRole, String identifier, String password) {
        boolean adminPreferred = "Admin".equalsIgnoreCase(preferredRole);
        boolean looksLikeEmail = identifier != null && identifier.contains("@");

        // The login screen allows quick demo switching, so this fallback avoids locking users into the wrong role choice.
        if (adminPreferred) {
            try {
                return loginAdmin(identifier, password);
            } catch (BusinessException adminFailure) {
                if (looksLikeEmail) {
                    return loginCustomer(identifier, password);
                }
                throw adminFailure;
            }
        }

        try {
            return loginCustomer(identifier, password);
        } catch (BusinessException customerFailure) {
            if (!looksLikeEmail) {
                return loginAdmin(identifier, password);
            }
            throw customerFailure;
        }
    }
}
