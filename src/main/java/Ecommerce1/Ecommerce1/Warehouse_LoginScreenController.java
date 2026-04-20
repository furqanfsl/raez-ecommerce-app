package Ecommerce1.Ecommerce1;
import Ecommerce1.Ecommerce1.service.Warehouse_AuthService;
import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

/**
 * LoginScreenController — handles login UI events only.
 * All auth logic delegated to AuthService.
 * Includes demo credentials button for easy testing.
 */
public class Warehouse_LoginScreenController {

    // CHANGED: interface now passes role and userID instead of just type and email
    public interface OnLoginListener {
        void onLogin(String role, String email, int userID);
    }

    private OnLoginListener onLoginListener;
    private final Warehouse_AuthService warehouse_AuthService = new Warehouse_AuthService();

    public void setOnLoginListener(OnLoginListener listener) {
        this.onLoginListener = listener;
    }

    @FXML private Label         titleLabel;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Text          footerText;
    @FXML private Button        demoBtn;

    private String activeTab = "staff";

    // ── Demo credentials ──
    // CHANGED: updated demo credentials to match unified seed data
    private static final String DEMO_STAFF_EMAIL    = "admin@raez.org.uk.wh";
    private static final String DEMO_STAFF_PASSWORD = "admin123";
    private static final String DEMO_USER_EMAIL     = "staff@raez.org.uk";
    private static final String DEMO_USER_PASSWORD  = "staff123";

    @FXML
    private void initialize() {
        setStaffTab();
        clearForm();
    }

    @FXML private void onStaffTab() { setStaffTab(); clearForm(); }
    @FXML private void onUserTab()  { setUserTab();  clearForm(); }

    /**
     * Auto-fills the email and password fields with demo credentials
     * based on whichever tab is currently active.
     */
    @FXML
    private void onDemoCredentials() {
        if ("staff".equals(activeTab)) {
            emailField.setText(DEMO_STAFF_EMAIL);
            passwordField.setText(DEMO_STAFF_PASSWORD);
        } else {
            emailField.setText(DEMO_USER_EMAIL);
            passwordField.setText(DEMO_USER_PASSWORD);
        }
        Warehouse_DialogUtil.hideError(errorLabel);
    }

    @FXML
    private void onLogin() {
        Warehouse_DialogUtil.hideError(errorLabel);
        String email    = emailField.getText()    == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "Please fill in all fields.");
            return;
        }

        Warehouse_AuthService.AuthResult result;

        if ("staff".equals(activeTab)) {
            // CHANGED: authenticateStaff now returns role + userID
            result = warehouse_AuthService.authenticateStaff(email, password);
        } else {
            // CHANGED: authenticateUser now returns role + userID
            result = warehouse_AuthService.authenticateUser(email, password);
        }

        if (!result.isSuccess()) {
            Warehouse_DialogUtil.showError(errorLabel, result.getMessage());
            return;
        }

        // CHANGED: now passes role and userID to App.java
        if (onLoginListener != null)
            onLoginListener.onLogin(result.getRole(), result.getEmail(), result.getUserID());
    }

    private void setStaffTab() {
        activeTab = "staff";
        titleLabel.setText("Warehouse Staff Login");
        footerText.setText("Authorised warehouse staff only.");
        emailField.setPromptText("name@raez.org.uk");
        if (demoBtn != null)
            demoBtn.setText("🔑  Demo: admin@raez.org.uk.wh / password");
    }

    private void setUserTab() {
        activeTab = "user";
        titleLabel.setText("User Login");
        footerText.setText("General warehouse user access.");
        emailField.setPromptText("name@raez.org.uk");
        if (demoBtn != null)
            demoBtn.setText("🔑  Demo: staff@raez.org.uk / password");
    }

    private void clearForm() {
        emailField.clear();
        passwordField.clear();
        Warehouse_DialogUtil.hideError(errorLabel);
    }
}