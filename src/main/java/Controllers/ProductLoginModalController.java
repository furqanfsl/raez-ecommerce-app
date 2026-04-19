package Controllers;

import com.reaz.db.DBConnection;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

public class ProductLoginModalController implements Initializable {

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
        if (demoEmailLabel    != null) demoEmailLabel.setText("Use an email and password from your database seed.");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Passwords are verified with passwordHash in the users table.");
        if (adminEmailHint    != null) { adminEmailHint.setVisible(false); adminEmailHint.setManaged(false); }
        if (emailField        != null) emailField.setPromptText("your@email.com");
        if (submitBtn         != null) submitBtn.setText("Login as Customer");
        clearError();
    }

    private void applyAdminTab() {
        if (adminTabBtn       != null) adminTabBtn.setStyle(activeTabStyle());
        if (customerTabBtn    != null) customerTabBtn.setStyle(inactiveTabStyle());
        if (demoEmailLabel    != null) demoEmailLabel.setText("Admin: account must have product_admin or super_admin role.");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Passwords are verified with passwordHash in the users table.");
        if (adminEmailHint    != null) { adminEmailHint.setVisible(true); adminEmailHint.setManaged(true); }
        if (emailField        != null) emailField.setPromptText("admin@raez.com");
        if (submitBtn         != null) submitBtn.setText("Login as Admin");
        clearError();
    }

    @FXML private void handleClearForm() {
        if (emailField    != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        clearError();
    }

    @FXML private void handleFillDemo() {
        if (emailField == null || passwordField == null) return;

        if ("admin".equals(activeTab)) {
            emailField.setText("admin@raez.com");
            passwordField.setText("admin123");
        } else {
            emailField.setText("customer@example.com");
            passwordField.setText("customer123");
        }
        clearError();
    }

    @FXML private void handleLogin() {
        String email    = emailField    != null ? emailField.getText().trim()    : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        String hashedPassword = DBConnection.hashPassword(password);
        String sql = """
            SELECT u.userID, u.firstName, u.lastName, u.email,
                   u.username, u.isActive, r.roleName
            FROM users u
            JOIN user_roles ur ON ur.userID = u.userID
            JOIN roles r ON r.roleID = ur.roleID
            WHERE u.email = ? AND u.passwordHash = ?
            """;

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashedPassword);
            ResultSet rs = ps.executeQuery();

            Set<String> roleNames = new LinkedHashSet<>();
            Integer userID = null;
            String firstName = null;
            String lastName = null;
            String em = null;
            String username = null;
            int isActive = 1;

            while (rs.next()) {
                if (userID == null) {
                    userID = rs.getInt("userID");
                    firstName = rs.getString("firstName");
                    lastName = rs.getString("lastName");
                    em = rs.getString("email");
                    username = rs.getString("username");
                    isActive = rs.getInt("isActive");
                }
                roleNames.add(rs.getString("roleName"));
            }

            if (userID == null) {
                showError("Invalid email or password.");
                return;
            }

            if (activeTab.equals("admin")) {
                boolean allowed = roleNames.stream().anyMatch(r ->
                    "product_admin".equals(r) || "super_admin".equals(r));
                if (!allowed) {
                    showError("Invalid credentials for this login type.");
                    return;
                }
                String roleName = roleNames.stream()
                    .filter(r -> "product_admin".equals(r) || "super_admin".equals(r))
                    .findFirst()
                    .orElse("product_admin");
                User user = new User(
                    userID,
                    firstName,
                    lastName,
                    em,
                    roleName,
                    isActive,
                    username,
                    null
                );
                if (onClose != null) onClose.run();
                if (onLoginSuccess != null) onLoginSuccess.accept(user);
                navigateToAdmin();
            } else if (activeTab.equals("customer")) {
                if (!roleNames.contains("customer")) {
                    showError("Invalid credentials for this login type.");
                    return;
                }
                User user = new User(
                    userID,
                    firstName,
                    lastName,
                    em,
                    "customer",
                    isActive,
                    username,
                    null
                );
                if (onClose != null) onClose.run();
                if (onLoginSuccess != null) onLoginSuccess.accept(user);
            } else {
                showError("Invalid email or password.");
            }
        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToAdmin() {
        try {
            if (submitBtn == null || submitBtn.getScene() == null) return;
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/ProductAdminDashboard.fxml"));
            submitBtn.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("Navigate to admin failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleClose() {
        if (onClose != null) onClose.run();
    }

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
}
