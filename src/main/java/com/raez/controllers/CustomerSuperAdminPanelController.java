package com.raez.controllers;

import com.raez.customer.dao.CustomerAdminDAO;
import com.raez.customer.model.CustomerUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CustomerSuperAdminPanelController {

    @FXML private TextField searchEmailField;
    @FXML private Label     searchErrorLabel;

    @FXML private VBox  customerInfoPane;
    @FXML private VBox  updateEmailPane;
    @FXML private VBox  updateIdCardPane;
    @FXML private VBox  helpPane;

    @FXML private Label customerIdLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerCurrentEmailLabel;
    @FXML private Label customerIdCardLabel;

    @FXML private TextField newEmailField;
    @FXML private Label     emailUpdateErrorLabel;
    @FXML private TextField newIdCardField;

    private final CustomerAdminDAO adminDAO = new CustomerAdminDAO();
    private CustomerUser adminUser;

    // [customerId, userId] of the currently found customer
    private int[] foundCustomer = null;
    // cached display name for confirm dialogs
    private String foundName    = null;

    public void setAdminUser(CustomerUser admin) { this.adminUser = admin; }

    @FXML
    public void initialize() {
        showCustomerPanes(false);
    }

    @FXML
    private void handleSearch() {
        searchErrorLabel.setText("");
        String email = searchEmailField.getText().trim();
        if (email.isEmpty()) {
            searchErrorLabel.setText("Please enter an email to search.");
            return;
        }
        try {
            foundCustomer = adminDAO.getCustomerByEmail(email);
            if (foundCustomer == null) {
                searchErrorLabel.setText("Customer not found.");
                showCustomerPanes(false);
                return;
            }

            // Load name from customers table via userID
            var customers = adminDAO.getAllCustomers();
            foundName = customers.stream()
                .filter(u -> u.getId() == foundCustomer[1])
                .map(CustomerUser::getName)
                .findFirst()
                .orElse("Unknown");

            customerIdLabel.setText("CUST-" + foundCustomer[0]);
            customerNameLabel.setText(foundName);
            customerCurrentEmailLabel.setText(email);

            // Load idCardImage from DAO
            int[] ids = adminDAO.getCustomerByEmail(email);
            customerIdCardLabel.setText(ids != null ? "(see profile)" : "N/A");

            showCustomerPanes(true);
        } catch (Exception e) {
            searchErrorLabel.setText("Search error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateEmail() {
        emailUpdateErrorLabel.setText("");
        if (foundCustomer == null) return;

        String newEmail = newEmailField.getText().trim();
        if (newEmail.isEmpty()) {
            emailUpdateErrorLabel.setText("Please enter a new email.");
            return;
        }
        if (!newEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            emailUpdateErrorLabel.setText("Invalid email format.");
            return;
        }
        if (newEmail.equalsIgnoreCase(customerCurrentEmailLabel.getText())) {
            emailUpdateErrorLabel.setText("New email is the same as current email.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Email Update");
        confirm.setHeaderText("Update Email for " + foundName);
        confirm.setContentText(
            "Old email: " + customerCurrentEmailLabel.getText() + "\n" +
            "New email: " + newEmail + "\n\nThis requires Super Admin privileges. Continue?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int adminId = adminUser != null ? adminUser.getId() : 0;
                    adminDAO.updateCustomerEmail(foundCustomer[0], newEmail, adminId);
                    customerCurrentEmailLabel.setText(newEmail);
                    newEmailField.clear();
                    new Alert(Alert.AlertType.INFORMATION, "Email updated successfully.")
                        .showAndWait();
                } catch (Exception e) {
                    emailUpdateErrorLabel.setText("Update failed: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleUpdateIdCard() {
        if (foundCustomer == null) return;
        String newIdCard = newIdCardField.getText().trim();
        if (newIdCard.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please enter a new ID card filename.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm ID Card Update");
        confirm.setHeaderText("Replace ID Card for " + foundName);
        confirm.setContentText("This requires Super Admin privileges. Continue?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int adminId = adminUser != null ? adminUser.getId() : 0;
                    adminDAO.updateCustomerIdCard(foundCustomer[0], newIdCard, adminId);
                    customerIdCardLabel.setText(newIdCard);
                    newIdCardField.clear();
                    new Alert(Alert.AlertType.INFORMATION, "ID Card updated successfully.")
                        .showAndWait();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Update failed: " + e.getMessage())
                        .showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerSuperAdminDashboard.fxml"));
            Stage stage = (Stage) searchEmailField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerSuperAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(adminUser);
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showCustomerPanes(boolean show) {
        customerInfoPane.setVisible(show);  customerInfoPane.setManaged(show);
        updateEmailPane.setVisible(show);   updateEmailPane.setManaged(show);
        updateIdCardPane.setVisible(show);  updateIdCardPane.setManaged(show);
        helpPane.setVisible(!show);         helpPane.setManaged(!show);
    }
}
