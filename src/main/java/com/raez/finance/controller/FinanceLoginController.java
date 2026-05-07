package com.raez.finance.controller;

import com.raez.finance.model.FinanceUser;
import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.service.FinanceAuthService;
import com.raez.finance.service.FinanceAuthService.FirstLoginRequiredException;
import com.raez.finance.util.FinanceStageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinanceLoginController {
    private static final Logger log = LoggerFactory.getLogger(FinanceLoginController.class);


    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    private final FinanceAuthService authService = new FinanceAuthService();

    private FinanceUserRole expectedRole;

    public void configureForRole(FinanceUserRole role) {
        this.expectedRole = role;
        if (role == FinanceUserRole.ADMIN) {
            titleLabel.setText("Admin Login");
            subtitleLabel.setText("Restricted Access - Admin Only");
            loginButton.setText("Login as Admin");
        } else {
            titleLabel.setText("Finance User Login");
            subtitleLabel.setText("Finance & Reporting Access");
            loginButton.setText("Login as Finance User");
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        try {
            FinanceUser user = authService.login(email, password);

            if (expectedRole != null && user.getRole() != expectedRole) {
                errorLabel.setText("You do not have access to this area with this account.");
                return;
            }

            try {
                navigateToDashboard(event);
            } catch (Exception navEx) {
                String msg = navEx.getMessage() != null ? navEx.getMessage() : navEx.getClass().getSimpleName();
                errorLabel.setText("FinanceLogin succeeded but could not open dashboard: " + msg);
                log.error("{}", "=== Dashboard load error ===");
                log.error("Error", navEx);
                if (navEx.getCause() != null) {
                    log.error("{}", "Cause: " + navEx.getCause().getMessage());
                    log.error("Error", navEx.getCause());
                }
            }
        } catch (FirstLoginRequiredException e) {
            errorLabel.setText("First-time login detected. Please change your password.");
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Authentication failed. Please try again.";
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            errorLabel.setText(msg);
        }
    }

    @FXML
    private void handleBackToRoleSelection(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FinanceStageNavigator.navigate(stage, "/com/raez/finance/view/FinanceRoleSelection.fxml");
    }

    @FXML
    private void handleForgotPassword() {
        new Alert(Alert.AlertType.INFORMATION,
                "Password reset will be implemented in a later phase. For now, please contact an administrator.")
                .showAndWait();
    }

    @FXML
    private void handleDemoAdmin(ActionEvent event) {
        emailField.setText("admin@raez.com");
        passwordField.setText("password123");
        handleLogin(event);
    }

    @FXML
    private void handleDemoUser(ActionEvent event) {
        emailField.setText("finance@raez.com");
        passwordField.setText("password123");
        handleLogin(event);
    }

    private void navigateToDashboard(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FinanceStageNavigator.navigate(stage, "/com/raez/finance/view/FinanceMainLayout.fxml");
        stage.setTitle("RAEZ Finance – Main");
        FinanceStageNavigator.forceMaximizedLayout(stage);
    }
}
