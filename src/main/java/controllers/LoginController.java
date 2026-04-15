package controllers;

import database.LoginDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.SceneSwitcher;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        boolean valid = LoginDAO.isValidLogin(email, password);

        if (valid) {
            errorLabel.setText("");
            SceneSwitcher.switchScene("dashboard.fxml", emailField);
        } else {
            errorLabel.setText("Invalid email or password.");
        }
    }
}