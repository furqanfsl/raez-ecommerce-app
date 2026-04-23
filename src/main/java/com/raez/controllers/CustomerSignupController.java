package com.raez.controllers;

import com.raez.customer.dao.CustomerDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.application.Platform;

import java.io.File;

public class CustomerSignupController {

    @FXML private TextField     firstNameField;
    @FXML private Label         firstNameErrorLabel;
    @FXML private TextField     lastNameField;
    @FXML private Label         lastNameErrorLabel;
    @FXML private TextField     emailField;
    @FXML private Label         emailErrorLabel;
    @FXML private TextField     phoneField;
    @FXML private Label         phoneErrorLabel;
    @FXML private TextArea      addressField;
    @FXML private Label         idCardLabel;
    @FXML private Label         idCardErrorLabel;
    @FXML private PasswordField passwordField;
    @FXML private Label         passwordErrorLabel;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         confirmPasswordErrorLabel;
    @FXML private Label         generalErrorLabel;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private String idCardPath = null;

    @FXML
    private void handleUploadIdCard() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select ID Card Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(emailField.getScene().getWindow());
        if (file != null) {
            idCardPath = file.getAbsolutePath();
            idCardLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSignup() {
        clearErrors();
        if (!validate()) return;

        try {
            customerDAO.register(
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                idCardPath
            );
            generalErrorLabel.setStyle("-fx-text-fill: green;");
            generalErrorLabel.setText("Account created! You can now log in.");
            Stage stage = (Stage) emailField.getScene().getWindow();
            Platform.runLater(stage::close);
        } catch (Exception e) {
            generalErrorLabel.setStyle("-fx-text-fill: red;");
            generalErrorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    private boolean validate() {
        boolean ok = true;
        if (firstNameField.getText().trim().isEmpty()) {
            firstNameErrorLabel.setText("First name required."); ok = false;
        }
        if (lastNameField.getText().trim().isEmpty()) {
            lastNameErrorLabel.setText("Last name required."); ok = false;
        }
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            emailErrorLabel.setText("Email required."); ok = false;
        } else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            emailErrorLabel.setText("Invalid email format."); ok = false;
        }
        if (passwordField.getText().length() < 6) {
            passwordErrorLabel.setText("Password must be at least 6 characters."); ok = false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordErrorLabel.setText("Passwords do not match."); ok = false;
        }
        return ok;
    }

    private void clearErrors() {
        firstNameErrorLabel.setText("");
        lastNameErrorLabel.setText("");
        emailErrorLabel.setText("");
        phoneErrorLabel.setText("");
        passwordErrorLabel.setText("");
        confirmPasswordErrorLabel.setText("");
        idCardErrorLabel.setText("");
        generalErrorLabel.setText("");
    }
}
