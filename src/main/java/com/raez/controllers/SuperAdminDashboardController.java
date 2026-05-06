package com.raez.controllers;

import com.raez.dao.SmtpSettingsDAO;
import com.raez.dao.SuperAdminDAO;
import com.raez.model.NavigationRouter;
import com.raez.model.SmtpSettings;
import com.raez.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SuperAdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label activeUsersValue;
    @FXML private Label activeCustomersValue;
    @FXML private Label productCountValue;
    @FXML private Label orderCountValue;

    @FXML private TableView<User>           userTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colActive;
    @FXML private TableColumn<User, String> colLastLogin;

    @FXML private TableView<User>           credentialsTable;
    @FXML private TableColumn<User, String> credRoleCol;
    @FXML private TableColumn<User, String> credNameCol;
    @FXML private TableColumn<User, String> credEmailCol;
    @FXML private TableColumn<User, String> credPwdCol;

    @FXML private TextField     smtpHostField;
    @FXML private TextField     smtpPortField;
    @FXML private TextField     smtpUsernameField;
    @FXML private PasswordField smtpPasswordField;
    @FXML private TextField     smtpFromField;
    @FXML private TextField     smtpFromNameField;
    @FXML private CheckBox      smtpTlsBox;
    @FXML private CheckBox      smtpEnabledBox;
    @FXML private Label         smtpStatusLabel;

    private final SuperAdminDAO    dao     = new SuperAdminDAO();
    private final SmtpSettingsDAO  smtpDao = new SmtpSettingsDAO();
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            String name = user.firstName != null ? user.firstName : user.email;
            welcomeLabel.setText("Signed in as " + name);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureUserTable();
        configureCredentialsTable();
        refreshMetrics();
        refreshUsers();
        loadSmtp();
    }

    // ── Metrics & Users ────────────────────────────────────────────────────

    private void configureUserTable() {
        colId       .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().userID)));
        colName     .setCellValueFactory(c -> new SimpleStringProperty(
                nullToEmpty(c.getValue().firstName) + " " + nullToEmpty(c.getValue().lastName)));
        colEmail    .setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().email)));
        colRole     .setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().roleName)));
        colActive   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive == 1 ? "✓" : "✕"));
        if (colLastLogin != null)
            colLastLogin.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().lastLogin != null ? c.getValue().lastLogin.substring(0, Math.min(16, c.getValue().lastLogin.length())) : "—"));
    }

    private void configureCredentialsTable() {
        if (credentialsTable == null) return;
        credRoleCol .setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().roleName)));
        credNameCol .setCellValueFactory(c -> new SimpleStringProperty(
                nullToEmpty(c.getValue().firstName) + " " + nullToEmpty(c.getValue().lastName)));
        credEmailCol.setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().email)));
        credPwdCol  .setCellValueFactory(c -> new SimpleStringProperty("raez123"));
        refreshCredentials();
    }

    private void refreshCredentials() {
        if (credentialsTable == null) return;
        credentialsTable.setItems(FXCollections.observableArrayList(dao.listAllUsers()));
    }

    private void refreshMetrics() {
        activeUsersValue    .setText(String.valueOf(dao.countActiveUsers()));
        activeCustomersValue.setText(String.valueOf(dao.countActiveCustomers()));
        productCountValue   .setText(String.valueOf(dao.countProducts()));
        orderCountValue     .setText(String.valueOf(dao.countOrders()));
    }

    private void refreshUsers() {
        List<User> users = dao.listAllUsers();
        userTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    private void handleRefresh() { refreshMetrics(); refreshUsers(); }

    @FXML
    private void handleAddUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create New User");
        dialog.setHeaderText("Add a system user and assign a role.");

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField firstName = new TextField();        firstName.setPromptText("First name");
        TextField lastName  = new TextField();        lastName.setPromptText("Last name");
        TextField email     = new TextField();        email.setPromptText("email@raez.org.uk");
        TextField username  = new TextField();        username.setPromptText("username");
        PasswordField pwd   = new PasswordField();    pwd.setPromptText("Initial password");
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(dao.listRoleNames()));
        role.getSelectionModel().selectFirst();

        grid.add(new Label("First name:"), 0, 0); grid.add(firstName, 1, 0);
        grid.add(new Label("Last name:"),  0, 1); grid.add(lastName,  1, 1);
        grid.add(new Label("Email:"),      0, 2); grid.add(email,     1, 2);
        grid.add(new Label("Username:"),   0, 3); grid.add(username,  1, 3);
        grid.add(new Label("Password:"),   0, 4); grid.add(pwd,       1, 4);
        grid.add(new Label("Role:"),       0, 5); grid.add(role,      1, 5);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(firstName::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn != createType) return null;
            if (email.getText().isBlank() || username.getText().isBlank() ||
                pwd.getText().isBlank()   || role.getValue() == null) {
                alert(Alert.AlertType.ERROR, "All fields except name are required."); return null;
            }
            int id = dao.createUser(email.getText().trim(), username.getText().trim(),
                                    pwd.getText(), firstName.getText().trim(),
                                    lastName.getText().trim(), role.getValue());
            if (id < 0) { alert(Alert.AlertType.ERROR, "Failed to create user (email/username already exists?)."); return null; }
            User u = new User(); u.userID = id; return u;
        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent()) { refreshUsers(); refreshMetrics(); }
    }

    @FXML
    private void handleToggleActive() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) { alert(Alert.AlertType.WARNING, "Select a user first."); return; }
        if (dao.setUserActive(u.userID, u.isActive != 1)) { refreshUsers(); refreshMetrics(); }
    }

    @FXML
    private void handleDeleteUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) { alert(Alert.AlertType.WARNING, "Select a user first."); return; }
        if (currentUser != null && u.userID == currentUser.userID) {
            alert(Alert.AlertType.WARNING, "You cannot delete your own account."); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete user " + u.email + "? This is irreversible.",
            ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            if (dao.deleteUser(u.userID)) { refreshUsers(); refreshMetrics(); }
        }
    }

    // ── SMTP ───────────────────────────────────────────────────────────────

    private void loadSmtp() {
        SmtpSettings s = smtpDao.load();
        smtpHostField    .setText(s.host        != null ? s.host        : "");
        smtpPortField    .setText(String.valueOf(s.port));
        smtpUsernameField.setText(s.username    != null ? s.username    : "");
        smtpPasswordField.setText(s.password    != null ? s.password    : "");
        smtpFromField    .setText(s.fromAddress != null ? s.fromAddress : "");
        if (smtpFromNameField != null)
            smtpFromNameField.setText(s.fromName != null ? s.fromName : "RAEZ");
        smtpTlsBox       .setSelected(s.useTls);
        smtpEnabledBox   .setSelected(s.isEnabled);
    }

    @FXML
    private void handleSaveSmtp() {
        SmtpSettings s = buildSmtpFromFields();
        if (smtpDao.save(s)) {
            smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a;");
            smtpStatusLabel.setText("SMTP settings saved.");
        } else {
            smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626;");
            smtpStatusLabel.setText("Failed to save SMTP settings.");
        }
    }

    @FXML
    private void handleTestSmtp() {
        SmtpSettings s = buildSmtpFromFields();
        if (!s.isEnabled || s.host == null || s.host.isBlank()) {
            smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #f59e0b;");
            smtpStatusLabel.setText("SMTP is not enabled — tick \"Enable email sending\" and Save first.");
            return;
        }
        // Send to the SMTP username (the real inbox); fromAddress may be an alias
        String dest = s.username != null && !s.username.isBlank() ? s.username : s.fromAddress;
        if (dest == null || dest.isBlank()) {
            smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626;");
            smtpStatusLabel.setText("Fill in the Username field first — the test email goes there.");
            return;
        }
        smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");
        smtpStatusLabel.setText("Sending test email to " + dest + " …");
        new Thread(() -> {
            boolean ok = com.raez.service.EmailService.send(
                dest, "RAEZ SMTP Test", "If you receive this, your SMTP settings are working correctly.");
            Platform.runLater(() -> {
                if (ok) {
                    smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a;");
                    smtpStatusLabel.setText("Test email sent to " + dest + ".");
                } else {
                    smtpStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626;");
                    smtpStatusLabel.setText("Test failed — check host/port/credentials or server logs.");
                }
            });
        }, "raez-smtp-test").start();
    }

    private SmtpSettings buildSmtpFromFields() {
        SmtpSettings s = new SmtpSettings();
        s.host        = smtpHostField.getText().trim();
        s.username    = smtpUsernameField.getText().trim();
        s.password    = smtpPasswordField.getText();
        s.fromAddress = smtpFromField.getText().trim();
        s.fromName    = smtpFromNameField != null && !smtpFromNameField.getText().isBlank()
                        ? smtpFromNameField.getText().trim() : "RAEZ";
        s.useTls      = smtpTlsBox.isSelected();
        s.isEnabled   = smtpEnabledBox.isSelected();
        try { s.port = Integer.parseInt(smtpPortField.getText().trim()); }
        catch (NumberFormatException e) { s.port = 587; }
        return s;
    }

    // ── Module navigation ──────────────────────────────────────────────────

    @FXML private void openProducts()       { NavigationRouter.getInstance().navigateTo("/fxml/ProductAdminDashboard.fxml"); }
    @FXML private void openCustomers()      { NavigationRouter.getInstance().navigateTo("/fxml/CustomerAdminDashboard.fxml"); }
    @FXML private void openOrders()         { NavigationRouter.getInstance().navigateTo("/fxml/OrdersDashboard.fxml"); }
    @FXML private void openWarehouse()      { NavigationRouter.getInstance().navigateTo("/fxml/WarehouseStaffDashboard.fxml"); }
    @FXML private void openDeliveries()     { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesDashboard.fxml"); }
    @FXML private void openFinance()        { NavigationRouter.getInstance().navigateTo("/com/raez/finance/view/FinanceMainLayout.fxml"); }
    @FXML private void openReviews()        { NavigationRouter.getInstance().navigateTo("/fxml/reviews-admin-dashboard.fxml"); }
    @FXML private void openStorefront()     { NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml"); }
    @FXML private void handleGoToStorefront() { NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml"); }

    @FXML
    private void handleEditUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) { alert(Alert.AlertType.WARNING, "Select a user first."); return; }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Update details for " + nullToEmpty(u.email));

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField firstName = new TextField(nullToEmpty(u.firstName));
        TextField lastName  = new TextField(nullToEmpty(u.lastName));
        TextField email     = new TextField(nullToEmpty(u.email));
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(dao.listRoleNames()));
        role.setValue(u.roleName);

        grid.add(new Label("First name:"), 0, 0); grid.add(firstName, 1, 0);
        grid.add(new Label("Last name:"),  0, 1); grid.add(lastName,  1, 1);
        grid.add(new Label("Email:"),      0, 2); grid.add(email,     1, 2);
        grid.add(new Label("Role:"),       0, 3); grid.add(role,      1, 3);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(firstName::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn != saveType) return null;
            if (email.getText().isBlank() || role.getValue() == null) {
                alert(Alert.AlertType.ERROR, "Email and role are required."); return null;
            }
            boolean ok = dao.updateUser(u.userID, email.getText().trim(),
                    firstName.getText().trim(), lastName.getText().trim(), role.getValue());
            if (!ok) { alert(Alert.AlertType.ERROR, "Failed to update user."); return null; }
            return Boolean.TRUE;
        });

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && Boolean.TRUE.equals(result.get())) { refreshUsers(); refreshCredentials(); }
    }

    @FXML
    private void handleLogout() { NavigationRouter.getInstance().logout(); }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
