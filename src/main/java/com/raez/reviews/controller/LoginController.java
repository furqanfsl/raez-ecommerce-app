package com.raez.reviews.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.UserSession;
import com.raez.reviews.util.UiUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String GENERIC_LOGIN_ERROR = "The application could not complete the login request.";

    @FXML
    private ChoiceBox<String> roleChoiceBox;
    @FXML
    private TextField identifierField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label roleSummaryLabel;
    @FXML
    private Label guidanceLabel;
    @FXML
    private Label statusLabel;

    private ReviewsApplication application;
    private AppContext appContext;

    public void init(ReviewsApplication application, AppContext appContext) {
        this.application = application;
        this.appContext = appContext;
        roleChoiceBox.setItems(FXCollections.observableArrayList("Customer", "Admin"));
        roleChoiceBox.getSelectionModel().selectFirst();
        updatePrompts();
        setStatusMessage(null);
    }

    @FXML
    private void initialize() {
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        statusLabel.setVisible(false);
        roleChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updatePrompts());
    }

    @FXML
    private void handleLogin() {
        setStatusMessage(null);
        try {
            // Fall back across roles so demo users are not blocked by the wrong selector.
            openWorkspace(appContext.getAuthService()
                    .loginWithFallback(roleChoiceBox.getValue(), identifierField.getText(), passwordField.getText()));
        } catch (BusinessException exception) {
            setStatusMessage(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Unable to complete the login workflow.", exception);
            setStatusMessage(GENERIC_LOGIN_ERROR);
        }
    }

    @FXML
    private void handleUseAdminDemo() {
        roleChoiceBox.setValue("Admin");
        identifierField.setText("reviews_admin_user");
        passwordField.setText("reviews123");
        setStatusMessage(null);
    }

    @FXML
    private void handleUseCustomerDemo() {
        roleChoiceBox.setValue("Customer");
        identifierField.setText("alice@raez.com");
        passwordField.setText("alice123");
        setStatusMessage(null);
    }

    @FXML
    private void handleShowDemoData() {
        UiUtils.showInfo(getOwnerWindow(), "Demo Data", """
                Shared Admin Accounts
                reviews_admin_user / reviews123
                superadmin / admin123

                Customer Accounts
                alice@raez.com / alice123
                omar@raez.com / omar123
                sara@raez.com / sara123
                maya@raez.com / maya123
                zaid@raez.com / zaid123
                """);
    }

    private void updatePrompts() {
        boolean adminMode = "Admin".equals(roleChoiceBox.getValue());
        identifierField.setPromptText(adminMode ? "Username" : "Email");
        // Swap the guidance text with the selected role so the seeded accounts are easier to test.
        roleSummaryLabel.setText(adminMode
                ? "Moderate review content, inspect the audit log, and manage the configurable edit window."
                : "Review purchased products, vote on feedback, and edit or delete your own review before the timer expires.");
        guidanceLabel.setText(adminMode
                ? "Use the shared administrator account to test moderation actions, status changes, and settings updates."
                : "Use a shared customer account to test purchase validation, review submission, helpfulness voting, and timed editing rules.");
    }

    private void openWorkspace(UserSession session) {
        if (session.isAdmin()) {
            application.showAdminDashboard(session);
        } else {
            application.showCustomerDashboard(session);
        }
    }

    private void setStatusMessage(String message) {
        boolean hasMessage = message != null && !message.isBlank();
        statusLabel.setText(hasMessage ? message : "");
        statusLabel.setVisible(hasMessage);
    }

    private Window getOwnerWindow() {
        return identifierField.getScene() == null ? null : identifierField.getScene().getWindow();
    }
}
