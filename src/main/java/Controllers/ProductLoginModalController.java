package Controllers;

import com.reaz.model.NavigationRouter;
import com.reaz.model.User;
import com.reaz.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
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

    private String   activeTab = "customer";
    private Runnable onClose;

    /** Optional: kept for backward compatibility but routing is handled by NavigationRouter. */
    @SuppressWarnings("unused")
    private Consumer<User> onLoginSuccess;

    public void setup(Consumer<User> onLoginSuccess, Runnable onClose) {
        this.onLoginSuccess = onLoginSuccess;
        this.onClose        = onClose;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyCustomerTab();
    }

    @FXML private void handleCustomerTab() { activeTab = "customer"; applyCustomerTab(); }
    @FXML private void handleAdminTab()    { activeTab = "admin";    applyAdminTab();    }

    private void applyCustomerTab() {
        if (customerTabBtn != null) customerTabBtn.setStyle(activeTabStyle());
        if (adminTabBtn    != null) adminTabBtn.setStyle(inactiveTabStyle());
        if (demoEmailLabel != null) demoEmailLabel.setText("Customer account");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Use credentials stored in your database");
        if (adminEmailHint != null) { adminEmailHint.setVisible(false); adminEmailHint.setManaged(false); }
        if (emailField     != null) emailField.setPromptText("your@email.com");
        if (submitBtn      != null) submitBtn.setText("Login");
        clearError();
    }

    private void applyAdminTab() {
        if (adminTabBtn    != null) adminTabBtn.setStyle(activeTabStyle());
        if (customerTabBtn != null) customerTabBtn.setStyle(inactiveTabStyle());
        if (demoEmailLabel != null) demoEmailLabel.setText("Product or Customer admin");
        if (demoPasswordLabel != null) demoPasswordLabel.setText("Use credentials stored in your database");
        if (adminEmailHint != null) { adminEmailHint.setVisible(true); adminEmailHint.setManaged(true); }
        if (emailField     != null) emailField.setPromptText("your.admin@email.com");
        if (submitBtn      != null) submitBtn.setText("Login as Admin");
        clearError();
    }

    @FXML private void handleClearForm() {
        if (emailField    != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        clearError();
    }

    @FXML private void handleFillDemo() {
        if (emailField == null || passwordField == null) return;
        emailField.clear();
        passwordField.clear();
        clearError();
    }

    @FXML private void handleLogin() {
        String email    = emailField    != null ? emailField.getText().trim()    : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            var session = AuthService.authenticate(email, password);
            if (session.isEmpty()) {
                showError("Invalid email or password.");
                return;
            }
            AuthService.AuthenticatedSession s = session.get();
            Set<String> roleNames = s.allRoleNames();

            // ── Admin tab: only admin roles allowed ────────────────────────
            if ("admin".equals(activeTab)) {
                boolean allowed = roleNames.stream().anyMatch(r ->
                    "product_admin".equals(r) || "super_admin".equals(r) || "customer_admin".equals(r));
                if (!allowed) { showError("No admin role found for this account."); return; }
            }

            // ── Customer tab: only customer role allowed ───────────────────
            if ("customer".equals(activeTab) && !roleNames.contains("customer")) {
                showError("No customer account found for this email."); return;
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

    // ── HELPERS ────────────────────────────────────────────────────────────

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
