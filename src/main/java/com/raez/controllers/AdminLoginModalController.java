package com.raez.controllers;

import com.raez.model.NavigationRouter;
import com.raez.model.User;
import com.raez.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class AdminLoginModalController implements Initializable {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private Runnable onClose;

    public void setup(Runnable onClose) { this.onClose = onClose; }

    @Override
    public void initialize(URL location, ResourceBundle resources) { clearError(); }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }

        try {
            var session = AuthService.authenticate(email, password);
            if (session.isEmpty()) { showError("Invalid email or password."); return; }

            AuthService.AuthenticatedSession s = session.get();
            Set<String> roles = s.allRoleNames();

            boolean isAdmin = roles.stream().anyMatch(r ->
                "super_admin".equals(r) || "product_admin".equals(r) || "customer_admin".equals(r) ||
                "warehouse_admin".equals(r) || "delivery_admin".equals(r) ||
                "orders_admin".equals(r) || "orders_user".equals(r) ||
                "finance_admin".equals(r) || "finance_user".equals(r) ||
                "reviews_admin".equals(r));
            if (!isAdmin) {
                showError("This portal is for admin accounts only.");
                return;
            }

            User user = s.user();
            if (onClose != null) onClose.run();
            NavigationRouter.getInstance().routeAfterLogin(user);

        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleClose() { if (onClose != null) onClose.run(); }

    @FXML
    private void handleForgotPassword() {
        ForgotPasswordDialog.show(
            emailField.getScene() != null ? emailField.getScene().getWindow() : null);
    }

    // ── Demo quick-fill ────────────────────────────────────────────────────

    @FXML private void demoSuperAdmin()    { fill("admin@raez.org.uk",           "admin123"); }
    @FXML private void demoProduct()       { fill("adminProduct@raez.org.uk",    "raez123"); }
    @FXML private void demoCustomerAdmin() { fill("adminCustomer@raez.org.uk",   "raez123"); }
    @FXML private void demoOrders()        { fill("orders@raez.org.uk",          "raez123"); }
    @FXML private void demoWarehouse()     { fill("adminWarehouse@raez.org.uk",  "raez123"); }
    @FXML private void demoDelivery()      { fill("adminDelivery@raez.org.uk",   "raez123"); }
    @FXML private void demoFinance()       { fill("adminFinance@raez.org.uk",    "raez123"); }
    @FXML private void demoReviews()       { fill("adminReviews@raez.org.uk",    "AReviews@12345"); }

    private void fill(String email, String password) {
        emailField.setText(email);
        passwordField.setText(password);
        clearError();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
