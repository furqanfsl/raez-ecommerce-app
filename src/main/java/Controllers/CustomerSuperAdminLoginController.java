package Controllers;

import com.reaz.customer.dao.CustomerAdminDAO;
import com.reaz.customer.model.CustomerUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class CustomerSuperAdminLoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailErrorLabel;
    @FXML private Label         passwordErrorLabel;
    @FXML private Label         generalErrorLabel;

    private final Pattern emailPattern =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final CustomerAdminDAO adminDAO = new CustomerAdminDAO();

    @FXML
    private void handleClearFields() {
        emailField.clear();
        passwordField.clear();
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
        generalErrorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
        generalErrorLabel.setText("");

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (!emailPattern.matcher(email).matches()) {
            emailErrorLabel.setText("Invalid email format.");
            return;
        }
        if (password.isEmpty()) {
            passwordErrorLabel.setText("Password required.");
            return;
        }

        try {
            CustomerUser user = adminDAO.superAdminLogin(email, password);
            if (user == null) {
                generalErrorLabel.setText("Invalid credentials.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerSuperAdminDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerSuperAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(scene);
        } catch (Exception e) {
            generalErrorLabel.setText("Login error: " + e.getMessage());
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
