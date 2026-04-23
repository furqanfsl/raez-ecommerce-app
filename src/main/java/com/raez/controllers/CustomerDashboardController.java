package com.raez.controllers;

import com.raez.model.NavigationRouter;
import com.raez.customer.dao.CustomerDAO;
import com.raez.customer.dao.CustomerOrderDAO;
import com.raez.customer.dao.CustomerPreferenceDAO;
import com.raez.customer.model.CustomerOrder;
import com.raez.customer.model.CustomerPreference;
import com.raez.customer.model.CustomerProfile;
import com.raez.customer.model.CustomerUser;
import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomerDashboardController {

    // Header
    @FXML private Label customerNameLabel;
    @FXML private Label customerIdLabel;
    @FXML private Label customerStatusLabel;

    // Tab buttons
    @FXML private Button tabProfile;
    @FXML private Button tabPreferences;
    @FXML private Button tabOrders;
    @FXML private Button tabEligible;

    // Panes
    @FXML private VBox profilePane;
    @FXML private VBox preferencesPane;
    @FXML private VBox ordersPane;
    @FXML private VBox reviewEligiblePane;
    @FXML private VBox eligibleListPane;

    // Profile tab
    @FXML private Label         profileMessageLabel;
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private Label         phoneErrorLabel;
    @FXML private TextArea      addressField;
    @FXML private Button        toggleAccountBtn;

    // Preferences tab
    @FXML private Label         preferencesMessageLabel;
    @FXML private FlowPane      categoriesPane;
    @FXML private ComboBox<String> notificationsCombo;
    @FXML private TextArea      deliveryInstructionsField;

    // Orders tab
    @FXML private TableView<CustomerOrder>     ordersTable;
    @FXML private TableColumn<CustomerOrder, Integer> colOrderId;
    @FXML private TableColumn<CustomerOrder, String>  colRobotType;
    @FXML private TableColumn<CustomerOrder, String>  colDate;
    @FXML private TableColumn<CustomerOrder, String>  colStatus;
    @FXML private TableColumn<CustomerOrder, String>  colAmount;

    private final CustomerDAO            customerDAO    = new CustomerDAO();
    private final CustomerPreferenceDAO  preferenceDAO  = new CustomerPreferenceDAO();
    private final CustomerOrderDAO       orderDAO       = new CustomerOrderDAO();

    private CustomerUser    currentUser;
    private CustomerProfile currentProfile;

    private static final List<String> CATEGORIES = Arrays.asList(
        "Home Assistants", "Security Bots", "Educational", "Companions", "Industrial"
    );
    private static final List<String> NOTIFICATION_OPTIONS = Arrays.asList("EMAIL", "SMS", "NONE");

    public void setUser(CustomerUser user) {
        this.currentUser = user;
        loadData();
    }

    @FXML
    public void initialize() {
        notificationsCombo.setItems(FXCollections.observableArrayList(NOTIFICATION_OPTIONS));

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colRobotType.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("formattedAmount"));

        buildCategoryCheckboxes(new HashSet<>());
    }

    private void loadData() {
        customerNameLabel.setText(currentUser.getName());
        customerIdLabel.setText("ID: " + currentUser.getId());
        boolean active = currentUser.isActive();
        customerStatusLabel.setText(active ? "Active" : "Inactive");
        customerStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16; " +
            "-fx-background-radius: 20; -fx-background-color: " + (active ? "#DCFCE7;" : "#FEE2E2;") +
            " -fx-text-fill: " + (active ? "#166534;" : "#991B1B;"));
        toggleAccountBtn.setText(active ? "Deactivate Account" : "Activate Account");

        // Load users.firstName / lastName for edit fields
        try {
            CustomerUser fresh = customerDAO.getById(currentUser.getId());
            if (fresh != null) {
                String[] parts = fresh.getName().split(" ", 2);
                firstNameField.setText(parts[0]);
                lastNameField.setText(parts.length > 1 ? parts[1] : "");
            }
            emailField.setText(currentUser.getEmail());
        } catch (Exception e) { e.printStackTrace(); }

        // Load profile (phone, address)
        try {
            currentProfile = customerDAO.getProfile(currentUser.getId());
            if (currentProfile != null) {
                phoneField.setText(nvl(currentProfile.getPhone()));
                addressField.setText(nvl(currentProfile.getAddress()));
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Load preferences
        try {
            CustomerPreference prefs = preferenceDAO.getByUserId(currentUser.getId());
            if (prefs != null) {
                String saved = nvl(prefs.getPreferredCategories());
                Set<String> selected = new HashSet<>(Arrays.asList(saved.split(",")));
                buildCategoryCheckboxes(selected);
                String notif = prefs.getNotificationSettings();
                if (notif != null && NOTIFICATION_OPTIONS.contains(notif))
                    notificationsCombo.setValue(notif);
                deliveryInstructionsField.setText(nvl(prefs.getDeliveryInstructions()));
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Load orders
        try {
            ordersTable.setItems(FXCollections.observableArrayList(
                orderDAO.getOrdersByUserId(currentUser.getId())));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── TAB SWITCHING ──────────────────────────────────────────────────────
    @FXML private void handleTabProfile() {
        setActiveTab(profilePane, tabProfile);
        setInactiveTab(preferencesPane, tabPreferences);
        setInactiveTab(ordersPane, tabOrders);
        setInactiveTab(reviewEligiblePane, tabEligible);
    }

    @FXML private void handleTabPreferences() {
        setActiveTab(preferencesPane, tabPreferences);
        setInactiveTab(profilePane, tabProfile);
        setInactiveTab(ordersPane, tabOrders);
        setInactiveTab(reviewEligiblePane, tabEligible);
    }

    @FXML private void handleTabOrders() {
        setActiveTab(ordersPane, tabOrders);
        setInactiveTab(profilePane, tabProfile);
        setInactiveTab(preferencesPane, tabPreferences);
        setInactiveTab(reviewEligiblePane, tabEligible);
    }

    @FXML private void handleTabEligible() {
        setActiveTab(reviewEligiblePane, tabEligible);
        setInactiveTab(profilePane, tabProfile);
        setInactiveTab(preferencesPane, tabPreferences);
        setInactiveTab(ordersPane, tabOrders);
        loadEligibleForReview();
    }

    // ── SAVE PROFILE ───────────────────────────────────────────────────────
    @FXML
    private void handleSaveProfile() {
        profileMessageLabel.setText("");
        phoneErrorLabel.setText("");
        try {
            customerDAO.saveProfile(
                currentUser.getId(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                phoneField.getText().trim(),
                addressField.getText().trim()
            );
            profileMessageLabel.setStyle("-fx-text-fill: green;");
            profileMessageLabel.setText("Profile saved successfully.");
            currentUser.setName(firstNameField.getText().trim() + " " + lastNameField.getText().trim());
            customerNameLabel.setText(currentUser.getName());
        } catch (Exception e) {
            profileMessageLabel.setStyle("-fx-text-fill: red;");
            profileMessageLabel.setText("Save failed: " + e.getMessage());
        }
    }

    // ── TOGGLE ACCOUNT ─────────────────────────────────────────────────────
    @FXML
    private void handleToggleAccount() {
        try {
            customerDAO.toggleAccountStatus(currentUser.getId());
            boolean nowActive = !currentUser.isActive();
            currentUser.setStatus(nowActive ? "ACTIVE" : "INACTIVE");
            toggleAccountBtn.setText(nowActive ? "Deactivate Account" : "Activate Account");
            customerStatusLabel.setText(nowActive ? "Active" : "Inactive");
            profileMessageLabel.setStyle("-fx-text-fill: green;");
            profileMessageLabel.setText("Account status updated.");
        } catch (Exception e) {
            profileMessageLabel.setStyle("-fx-text-fill: red;");
            profileMessageLabel.setText("Error: " + e.getMessage());
        }
    }

    // ── SAVE PREFERENCES ───────────────────────────────────────────────────
    @FXML
    private void handleSavePreferences() {
        preferencesMessageLabel.setText("");
        try {
            StringBuilder selected = new StringBuilder();
            for (javafx.scene.Node node : categoriesPane.getChildren()) {
                if (node instanceof CheckBox cb && cb.isSelected()) {
                    if (selected.length() > 0) selected.append(",");
                    selected.append(cb.getText());
                }
            }
            String notification = notificationsCombo.getValue() != null
                ? notificationsCombo.getValue() : "NONE";
            preferenceDAO.savePreferences(
                currentUser.getId(),
                selected.toString(),
                notification,
                deliveryInstructionsField.getText().trim()
            );
            preferencesMessageLabel.setStyle("-fx-text-fill: green;");
            preferencesMessageLabel.setText("Preferences saved.");
        } catch (Exception e) {
            preferencesMessageLabel.setStyle("-fx-text-fill: red;");
            preferencesMessageLabel.setText("Save failed: " + e.getMessage());
        }
    }

    // ── ELIGIBLE FOR REVIEW ────────────────────────────────────────────────
    private void loadEligibleForReview() {
        eligibleListPane.getChildren().clear();
        try {
            List<CustomerOrder> eligible = orderDAO.getEligibleForReview(currentUser.getId());
            if (eligible.isEmpty()) {
                Label empty = new Label("No products eligible for review at the moment.\n" +
                    "Products become eligible once your order is delivered (within 30 days).");
                empty.setWrapText(true);
                empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-padding: 20 0;");
                eligibleListPane.getChildren().add(empty);
                return;
            }
            for (CustomerOrder co : eligible) {
                eligibleListPane.getChildren().add(buildEligibleCard(co));
            }
        } catch (Exception e) {
            Label err = new Label("Could not load eligible products: " + e.getMessage());
            err.setStyle("-fx-text-fill: red;");
            eligibleListPane.getChildren().add(err);
        }
    }

    private HBox buildEligibleCard(CustomerOrder co) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB;" +
                      "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 16;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(co.getProductName() != null ? co.getProductName() : "Product #" + co.getProductId());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label detail = new Label("Order #" + co.getOrderId() + "  •  " + nvl(co.getOrderDate()));
        detail.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        info.getChildren().addAll(name, detail);

        Button writeBtn = new Button("Write a Review");
        writeBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;" +
                          "-fx-padding: 8 16; -fx-cursor: hand; -fx-border-radius: 4; -fx-background-radius: 4;");
        int productId = co.getProductId();
        String productName = co.getProductName();
        writeBtn.setOnAction(e -> openReviewForm(productId, productName));

        card.getChildren().addAll(info, writeBtn);
        return card;
    }

    private void openReviewForm(int productId, String productName) {
        try {
            int customerId = customerDAO.getCustomerIdByUserId(currentUser.getId());
            if (customerId < 0) {
                showAlert("Could not find your customer record.");
                return;
            }
            ReviewsApplication app = new ReviewsApplication(
                eligibleListPane.getScene().getWindow());
            app.openReviewDialog("Review: " + productName, null, false, draft -> {
                try {
                    AppContext.getInstance().getReviewService()
                        .createReview(customerId, productId, draft.getRating(), draft.getComment());
                    loadEligibleForReview();
                } catch (Exception ex) {
                    showAlert("Could not save review: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            showAlert("Error opening review form: " + ex.getMessage());
        }
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ── LOGOUT ─────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        NavigationRouter.getInstance().logout();
    }

    // ── HELPERS ────────────────────────────────────────────────────────────
    private void buildCategoryCheckboxes(Set<String> selected) {
        categoriesPane.getChildren().clear();
        for (String cat : CATEGORIES) {
            CheckBox cb = new CheckBox(cat);
            cb.setSelected(selected.contains(cat));
            cb.setStyle("-fx-padding: 4 10; -fx-border-color: #E5E7EB; -fx-border-radius: 4; " +
                        "-fx-background-color: white; -fx-background-radius: 4;");
            categoriesPane.getChildren().add(cb);
        }
    }

    private void setActiveTab(VBox pane, Button btn) {
        pane.setVisible(true); pane.setManaged(true);
        btn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");
    }

    private void setInactiveTab(VBox pane, Button btn) {
        pane.setVisible(false); pane.setManaged(false);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #E5E7EB; -fx-padding: 8 20; -fx-cursor: hand;");
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
