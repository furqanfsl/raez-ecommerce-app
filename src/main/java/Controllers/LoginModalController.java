package Controllers;

import com.reaz.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class LoginModalController implements Initializable {

    @FXML private Button        customerTabBtn;
    @FXML private Button        adminTabBtn;
    @FXML private Label         demoEmailLabel;
    @FXML private Label         demoPasswordLabel;
    @FXML private Label         adminEmailHint;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        submitBtn;

    private String         activeTab = "customer";
    private Runnable       onClose;
    private Consumer<User> onLoginSuccess;

    public void setup(Consumer<User> onLoginSuccess, Runnable onClose) {
        this.onLoginSuccess = onLoginSuccess;
        this.onClose        = onClose;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyCustomerTab();
    }

    // ── Tab handlers ──────────────────────────────────────────────────────

    @FXML private void handleCustomerTab() {
        activeTab = "customer";
        applyCustomerTab();
    }

    @FXML private void handleAdminTab() {
        activeTab = "admin";
        applyAdminTab();
    }

    private void applyCustomerTab() {
        if (customerTabBtn    != null) customerTabBtn.setStyle(activeTabStyle());
        if (adminTabBtn       != null) adminTabBtn.setStyle(inactiveTabStyle());
        if (demoEmailLabel    != null) demoEmailLabel.setText("Email: customer@example.com");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Password: customer123");
        if (adminEmailHint    != null) { adminEmailHint.setVisible(false); adminEmailHint.setManaged(false); }
        if (emailField        != null) emailField.setPromptText("your@email.com");
        if (submitBtn         != null) submitBtn.setText("Login as Customer");
        clearError();
    }

    private void applyAdminTab() {
        if (adminTabBtn       != null) adminTabBtn.setStyle(activeTabStyle());
        if (customerTabBtn    != null) customerTabBtn.setStyle(inactiveTabStyle());
        if (demoEmailLabel    != null) demoEmailLabel.setText("Email: admin@raez.com");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Password: admin123");
        if (adminEmailHint    != null) { adminEmailHint.setVisible(true); adminEmailHint.setManaged(true); }
        if (emailField        != null) emailField.setPromptText("admin@raez.com");
        if (submitBtn         != null) submitBtn.setText("Login as Admin");
        clearError();
    }

    // ── Fill demo credentials ─────────────────────────────────────────────

    @FXML private void handleFillDemo() {
        if (activeTab.equals("admin")) {
            if (emailField    != null) emailField.setText("admin@raez.com");
            if (passwordField != null) passwordField.setText("admin123");
        } else {
            if (emailField    != null) emailField.setText("customer@example.com");
            if (passwordField != null) passwordField.setText("customer123");
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @FXML private void handleLogin() {
        String email    = emailField    != null ? emailField.getText().trim()    : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (activeTab.equals("admin")) {
            if (!email.equals("admin@raez.com") || !password.equals("admin123")) {
                showError("Invalid admin credentials.");
                return;
            }
            User user = new User(1, "Admin", email, "ADMIN", "ACTIVE");
            if (onClose        != null) onClose.run();
            if (onLoginSuccess != null) onLoginSuccess.accept(user);
            navigateToAdmin();

        } else {
            if (!email.equals("customer@example.com") || !password.equals("customer123")) {
                showError("Invalid email or password.");
                return;
            }
            User user = new User(2, "Customer", email, "CUSTOMER", "ACTIVE");
            if (onClose        != null) onClose.run();
            if (onLoginSuccess != null) onLoginSuccess.accept(user);
        }
    }

    // FIX: removed (BorderPane) cast — root is a VBox, use setRoot() instead
    private void navigateToAdmin() {
        try {
            if (submitBtn == null || submitBtn.getScene() == null) return;
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/AdminDashboard.fxml"));
            submitBtn.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("Navigate to admin failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────

    @FXML private void handleClose() {
        if (onClose != null) onClose.run();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showError(String message) {
        if (errorLabel == null) return;
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        if (errorLabel == null) return;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private String activeTabStyle() {
        return "-fx-background-color: #f9fafb;" +
               "-fx-border-color: transparent transparent #111827 transparent;" +
               "-fx-border-width: 0 0 2 0; -fx-font-size: 14; -fx-text-fill: #111827;" +
               "-fx-padding: 14 0 14 0; -fx-cursor: hand; -fx-background-radius: 0;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: white; -fx-border-color: transparent;" +
               "-fx-font-size: 14; -fx-text-fill: #6b7280;" +
               "-fx-padding: 14 0 14 0; -fx-cursor: hand; -fx-background-radius: 0;";
    }

    // Keep for backward compatibility
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}