package Controllers;

import com.reaz.customer.dao.CustomerDAO;
import com.reaz.customer.model.CustomerUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CustomerLoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailErrorLabel;
    @FXML private Label         passwordErrorLabel;
    @FXML private Label         generalErrorLabel;

    private final CustomerDAO customerDAO = new CustomerDAO();

    @FXML
    private void handleClearFields() {
        emailField.clear();
        passwordField.clear();
        clearErrors();
    }

    @FXML
    private void handleLogin() {
        clearErrors();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty())    { emailErrorLabel.setText("Email required.");    return; }
        if (password.isEmpty()) { passwordErrorLabel.setText("Password required."); return; }

        try {
            CustomerUser user = customerDAO.login(email, password);
            if (user == null) {
                generalErrorLabel.setText("Invalid email or password.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(scene);
        } catch (Exception e) {
            generalErrorLabel.setText("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        loadScene("/fxml/CustomerWelcome.fxml");
    }

    @FXML
    private void handleCreateAccount() {
        loadScene("/fxml/CustomerSignup.fxml");
    }

    private void loadScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), stage.getWidth(), stage.getHeight()));
        } catch (Exception e) {
            generalErrorLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    private void clearErrors() {
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
        generalErrorLabel.setText("");
    }
}
