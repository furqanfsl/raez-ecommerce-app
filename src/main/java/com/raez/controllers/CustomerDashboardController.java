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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerDashboardController {
    private static final Logger log = LoggerFactory.getLogger(CustomerDashboardController.class);


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
        customerStatusLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
            "-fx-background-radius: 20;" +
            "-fx-background-color: " + (active ? "rgba(94,234,212,0.18);" : "rgba(248,113,113,0.18);") +
            "-fx-border-color: "     + (active ? "rgba(94,234,212,0.45);" : "rgba(248,113,113,0.45);") +
            "-fx-border-radius: 20; -fx-border-width: 0.5;" +
            "-fx-text-fill: "        + (active ? "#5eead4;" : "#fca5a5;"));
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
        } catch (Exception e) { log.error("Error", e); }

        // Load profile (phone, address)
        try {
            currentProfile = customerDAO.getProfile(currentUser.getId());
            if (currentProfile != null) {
                phoneField.setText(nvl(currentProfile.getPhone()));
                addressField.setText(nvl(currentProfile.getAddress()));
            }
        } catch (Exception e) { log.error("Error", e); }

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
        } catch (Exception e) { log.error("Error", e); }

        // Load orders
        try {
            ordersTable.setItems(FXCollections.observableArrayList(
                orderDAO.getOrdersByUserId(currentUser.getId())));
        } catch (Exception e) { log.error("Error", e); }
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

    // ── ELIGIBLE FOR REVIEW + EXISTING REVIEWS ─────────────────────────────
    private void loadEligibleForReview() {
        eligibleListPane.getChildren().clear();
        int customerId = -1;
        try {
            customerId = customerDAO.getCustomerIdByUserId(currentUser.getId());
        } catch (Exception ignored) { /* no-op */ }

        // Section 1: products eligible for a NEW review (delivered within 30 days)
        try {
            List<CustomerOrder> eligible = orderDAO.getEligibleForReview(currentUser.getId());
            eligibleListPane.getChildren().add(buildSectionHeader(
                "Eligible for review",
                "Products delivered in the last 30 days you haven't reviewed yet."));
            if (eligible.isEmpty()) {
                eligibleListPane.getChildren().add(buildEmptyHint(
                    "Nothing new to review right now."));
            } else {
                for (CustomerOrder co : eligible) {
                    eligibleListPane.getChildren().add(buildEligibleCard(co));
                }
            }
        } catch (Exception e) {
            eligibleListPane.getChildren().add(buildErrorHint(
                "Could not load eligible products: " + e.getMessage()));
        }

        // Section 2: existing reviews — editable while still inside the 30-day window
        if (customerId >= 0) {
            try {
                List<com.raez.reviews.model.Review> existing =
                    com.raez.reviews.app.AppContext.getInstance().getReviewService()
                        .getReviewsByCustomer(customerId);
                eligibleListPane.getChildren().add(buildSectionHeader(
                    "Your reviews",
                    "Edit or delete within 30 days of writing."));
                if (existing.isEmpty()) {
                    eligibleListPane.getChildren().add(buildEmptyHint(
                        "You haven't written any reviews yet."));
                } else {
                    for (com.raez.reviews.model.Review r : existing) {
                        eligibleListPane.getChildren().add(buildExistingReviewCard(r, customerId));
                    }
                }
            } catch (Exception e) {
                eligibleListPane.getChildren().add(buildErrorHint(
                    "Could not load your reviews: " + e.getMessage()));
            }
        }
    }

    private Label buildSectionHeader(String title, String subtitle) {
        Label header = new Label(title.toUpperCase() + "  ·  " + subtitle);
        header.setWrapText(true);
        header.setStyle("-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
                        "-fx-font-size: 11; -fx-font-weight: bold;" +
                        "-fx-text-fill: #5eead4; -fx-letter-spacing: 2;" +
                        "-fx-padding: 8 0 2 2;");
        return header;
    }

    private Label buildEmptyHint(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;" +
                   "-fx-padding: 4 0 8 4;");
        return l;
    }

    private Label buildErrorHint(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");
        return l;
    }

    private HBox buildEligibleCard(CustomerOrder co) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04);" +
                      "-fx-border-color: rgba(255,255,255,0.10);" +
                      "-fx-border-radius: 14; -fx-background-radius: 14;" +
                      "-fx-border-width: 0.5; -fx-padding: 14 18;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(co.getProductName() != null ? co.getProductName() : "Product #" + co.getProductId());
        name.setStyle("-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
                      "-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: white;");
        Label detail = new Label("Order #" + co.getOrderId() + "  ·  " + nvl(co.getOrderDate()));
        detail.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");
        info.getChildren().addAll(name, detail);

        Button writeBtn = new Button("Write a review");
        writeBtn.setStyle("-fx-background-color: rgba(94,234,212,0.18);" +
                          "-fx-text-fill: #5eead4;" +
                          "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
                          "-fx-font-size: 12; -fx-font-weight: bold;" +
                          "-fx-padding: 8 18; -fx-cursor: hand;" +
                          "-fx-background-radius: 20;" +
                          "-fx-border-color: rgba(94,234,212,0.35);" +
                          "-fx-border-radius: 20; -fx-border-width: 0.5;");
        int productId = co.getProductId();
        String productName = co.getProductName();
        writeBtn.setOnAction(e -> openReviewForm(productId, productName));

        card.getChildren().addAll(info, writeBtn);
        return card;
    }

    private VBox buildExistingReviewCard(com.raez.reviews.model.Review r, int customerId) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04);" +
                      "-fx-border-color: rgba(255,255,255,0.10);" +
                      "-fx-border-radius: 14; -fx-background-radius: 14;" +
                      "-fx-border-width: 0.5; -fx-padding: 14 18;");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(r.getProductName() != null ? r.getProductName() : "Product #" + r.getProductId());
        name.setStyle("-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
                      "-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: white;");
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) stars.append(i <= r.getRating() ? '★' : '☆');
        Label starsLbl = new Label(stars + "  " + r.getRating() + "/5");
        starsLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(name, starsLbl, spacer);

        Label comment = new Label(r.getComment() == null || r.getComment().isBlank()
            ? "(no comment)" : r.getComment());
        comment.setWrapText(true);
        comment.setStyle("-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 12;");

        var svc = com.raez.reviews.app.AppContext.getInstance().getReviewService();
        boolean editable = svc.isEditableByCustomer(r, customerId);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        java.time.Duration remaining = svc.getRemainingEditDuration(r, customerId);
        Label window = new Label(formatRemaining(remaining));
        window.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.45);");

        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: rgba(94,234,212,0.16);" +
                         "-fx-text-fill: #5eead4;" +
                         "-fx-border-color: rgba(94,234,212,0.35);" +
                         "-fx-border-radius: 18; -fx-background-radius: 18;" +
                         "-fx-font-size: 12; -fx-font-weight: bold;" +
                         "-fx-padding: 6 14; -fx-cursor: hand; -fx-border-width: 0.5;");
        editBtn.setDisable(!editable);
        editBtn.setOnAction(e -> openExistingReviewEdit(r, customerId));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: rgba(248,113,113,0.16);" +
                           "-fx-text-fill: #fca5a5;" +
                           "-fx-border-color: rgba(248,113,113,0.35);" +
                           "-fx-border-radius: 18; -fx-background-radius: 18;" +
                           "-fx-font-size: 12; -fx-font-weight: bold;" +
                           "-fx-padding: 6 14; -fx-cursor: hand; -fx-border-width: 0.5;");
        deleteBtn.setDisable(!editable);
        deleteBtn.setOnAction(e -> deleteExistingReview(r, customerId));

        actions.getChildren().addAll(window, actSpacer, editBtn, deleteBtn);

        card.getChildren().addAll(top, comment, actions);
        return card;
    }

    private String formatRemaining(java.time.Duration remaining) {
        if (remaining == null || remaining.isZero() || remaining.isNegative()) {
            return "Edit window closed";
        }
        long days = remaining.toDays();
        if (days >= 1) return "Editable for " + days + "d " + (remaining.toHours() % 24) + "h";
        long mins = remaining.toMinutes();
        if (mins >= 60) return "Editable for " + (mins / 60) + "h " + (mins % 60) + "m";
        return "Editable for " + mins + "m";
    }

    private void openExistingReviewEdit(com.raez.reviews.model.Review r, int customerId) {
        ReviewsApplication app = new ReviewsApplication(eligibleListPane.getScene().getWindow());
        app.openReviewDialog("Edit Review", r, false, draft -> {
            try {
                AppContext.getInstance().getReviewService()
                    .editReview(customerId, r.getReviewId(), draft.getRating(), draft.getComment());
                loadEligibleForReview();
            } catch (Exception ex) {
                showAlert("Could not update review: " + ex.getMessage());
            }
        });
    }

    private void deleteExistingReview(com.raez.reviews.model.Review r, int customerId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this review? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Delete Review");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                AppContext.getInstance().getReviewService()
                    .deleteReview(customerId, r.getReviewId());
                loadEligibleForReview();
            } catch (Exception ex) {
                showAlert("Could not delete review: " + ex.getMessage());
            }
        });
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

    // ── BACK TO STOREFRONT ─────────────────────────────────────────────────
    @FXML
    private void handleBackToStore() {
        try {
            javafx.scene.Parent view = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/fxml/ProductHomepage.fxml"));
            firstNameField.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("Failed to navigate to storefront", e);
        }
    }

    // ── HELPERS ────────────────────────────────────────────────────────────
    private void buildCategoryCheckboxes(Set<String> selected) {
        categoriesPane.getChildren().clear();
        for (String cat : CATEGORIES) {
            CheckBox cb = new CheckBox(cat);
            cb.setSelected(selected.contains(cat));
            cb.setStyle(
                "-fx-padding: 6 12 6 8;" +
                "-fx-text-fill: rgba(255,255,255,0.85);" +
                "-fx-font-size: 12;" +
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.18);" +
                "-fx-border-radius: 18; -fx-border-width: 0.5;");
            categoriesPane.getChildren().add(cb);
        }
    }

    private void setActiveTab(VBox pane, Button btn) {
        pane.setVisible(true); pane.setManaged(true);
        btn.setStyle(
            "-fx-background-color: rgba(94,234,212,0.18);" +
            "-fx-text-fill: #5eead4;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-padding: 9 20; -fx-background-radius: 22;" +
            "-fx-border-color: rgba(94,234,212,0.35);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;");
    }

    private void setInactiveTab(VBox pane, Button btn) {
        pane.setVisible(false); pane.setManaged(false);
        btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: rgba(255,255,255,0.75);" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-padding: 9 20; -fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;");
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
