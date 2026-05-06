package com.raez.controllers;

import com.raez.model.NavigationRouter;
import com.raez.model.User;
import com.raez.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductLoginModalController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ProductLoginModalController.class);


    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        submitBtn;

    private Runnable onClose;

    @SuppressWarnings("unused")
    private Consumer<User> onLoginSuccess;

    public void setup(Consumer<User> onLoginSuccess, Runnable onClose) {
        this.onLoginSuccess = onLoginSuccess;
        this.onClose        = onClose;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearError();
    }

    @FXML
    private void handleLogin() {
        String email    = emailField    != null ? emailField.getText().trim()    : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }

        try {
            var session = AuthService.authenticate(email, password);
            if (session.isEmpty()) { showError("Invalid email or password."); return; }

            AuthService.AuthenticatedSession s = session.get();
            Set<String> roleNames = s.allRoleNames();

            if (!roleNames.contains("customer")) {
                showError("This modal is for customers only. Use the Admin Login link in the footer.");
                return;
            }

            User user = s.user();
            if (onClose != null) onClose.run();
            NavigationRouter.getInstance().routeAfterLogin(user);

        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
            log.error("Error", e);
        }
    }

    @FXML
    private void handleFillDemo() {
        if (emailField != null)    emailField.setText("alice@raez.com");
        if (passwordField != null) passwordField.setText("raez123");
        clearError();
    }

    @FXML
    private void handleForgotPassword() {
        ForgotPasswordDialog.show(
            emailField != null && emailField.getScene() != null
                ? emailField.getScene().getWindow() : null);
    }

    @FXML
    private void handleClose() { if (onClose != null) onClose.run(); }

    @FXML
    private void handleCreateAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerSignup.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (emailField != null && emailField.getScene() != null) {
                dialog.initOwner(emailField.getScene().getWindow());
            }
            dialog.setTitle("Create Account");
            // Cap the dialog at ~88% of the screen so the form's internal
            // ScrollPane handles the overflow instead of the dialog growing
            // taller than the desktop and hiding its bottom buttons.
            javafx.geometry.Rectangle2D vb =
                javafx.stage.Screen.getPrimary().getVisualBounds();
            double w = Math.min(640, vb.getWidth()  * 0.92);
            double h = Math.min(820, vb.getHeight() * 0.88);
            Scene scene = new Scene(root, w, h);
            dialog.setScene(scene);
            dialog.setMinWidth(420);
            dialog.setMinHeight(520);
            dialog.setResizable(true);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open registration: " + e.getMessage());
            log.error("Error", e);
        }
    }

    private void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        if (errorLabel == null) return;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
