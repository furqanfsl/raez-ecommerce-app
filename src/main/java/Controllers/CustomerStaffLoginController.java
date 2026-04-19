package Controllers;

import com.reaz.customer.dao.CustomerAdminDAO;
import com.reaz.customer.model.CustomerUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class CustomerStaffLoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final Pattern emailPattern =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final CustomerAdminDAO adminDAO = new CustomerAdminDAO();

    @FXML
    private void handleAutofill() {
        emailField.setText("admin@raez.com");
        passwordField.setText("admin123");
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        errorLabel.setText("");
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }
        if (!emailPattern.matcher(email).matches()) {
            errorLabel.setText("Invalid email format.");
            return;
        }

        try {
            CustomerUser user = adminDAO.staffLogin(email, password);
            if (user == null) {
                errorLabel.setText("Invalid credentials.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerAdminDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(scene);
        } catch (Exception e) {
            errorLabel.setText("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerWelcome.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), stage.getWidth(), stage.getHeight()));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
