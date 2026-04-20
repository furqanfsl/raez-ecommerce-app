package com.raez.reviews.controller;

import java.util.List;
import java.util.Optional;

import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.ModerationAuditEntry;
import com.raez.reviews.model.Product;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewStatus;
import com.raez.reviews.model.UserSession;
import com.raez.reviews.util.TimeUtils;
import com.raez.reviews.util.UiUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminDashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private ComboBox<Product> productFilterComboBox;
    @FXML
    private ComboBox<ReviewStatus> statusFilterComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Spinner<Integer> editWindowSpinner;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<Review> reviewTable;
    @FXML
    private TableColumn<Review, String> productColumn;
    @FXML
    private TableColumn<Review, String> customerColumn;
    @FXML
    private TableColumn<Review, Number> ratingColumn;
    @FXML
    private TableColumn<Review, String> reviewStatusColumn;
    @FXML
    private TableColumn<Review, Number> helpfulColumn;
    @FXML
    private TableColumn<Review, Number> unhelpfulColumn;
    @FXML
    private TableColumn<Review, String> updatedColumn;
    @FXML
    private TableColumn<Review, String> commentColumn;
    @FXML
    private TableView<ModerationAuditEntry> auditTable;
    @FXML
    private TableColumn<ModerationAuditEntry, String> auditTimeColumn;
    @FXML
    private TableColumn<ModerationAuditEntry, String> auditAdminColumn;
    @FXML
    private TableColumn<ModerationAuditEntry, Number> auditReviewColumn;
    @FXML
    private TableColumn<ModerationAuditEntry, String> auditActionColumn;
    @FXML
    private TableColumn<ModerationAuditEntry, String> auditReasonColumn;

    private ReviewsApplication application;
    private AppContext appContext;
    private UserSession session;

    @FXML
    private void initialize() {
        reviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        productColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        customerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        ratingColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRating()));
        reviewStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        helpfulColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getHelpfulCount()));
        unhelpfulColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getUnhelpfulCount()));
        updatedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(TimeUtils.toDisplay(cellData.getValue().getUpdatedAt())));
        commentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComment()));

        auditTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(TimeUtils.toDisplay(cellData.getValue().getActionTime())));
        auditAdminColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdminName()));
        auditReviewColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getReviewId()));
        auditActionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAction()));
        auditReasonColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReason()));

        editWindowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 5));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshAll());
        productFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshAll());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshAll());
        reviewTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateStatusLabel());
    }

    public void init(ReviewsApplication application, AppContext appContext, UserSession session) {
        this.application = application;
        this.appContext = appContext;
        this.session = session;
        welcomeLabel.setText("Admin session: " + session.getDisplayName());
        productFilterComboBox.setItems(FXCollections.observableArrayList(appContext.getReviewService().getProducts("")));
        statusFilterComboBox.setItems(FXCollections.observableArrayList(ReviewStatus.values()));
        editWindowSpinner.getValueFactory().setValue(appContext.getSettingsService().getReviewEditWindowMinutes());
        refreshAll();
    }

    @FXML
    private void handleSaveSettings() {
        try {
            appContext.getSettingsService().updateReviewEditWindowMinutes(editWindowSpinner.getValue());
            statusLabel.setText("The review edit window was updated.");
        } catch (BusinessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleFlagReview() {
        applyStatusAction("Flag Review", "Add a reason for flagging this review.", (review, reason) ->
                appContext.getModerationService().flagReview(session.getAdminUserId(), review.getReviewId(), reason));
    }

    @FXML
    private void handleRemoveReview() {
        applyStatusAction("Remove Review", "Add a reason for removing this review.", (review, reason) ->
                appContext.getModerationService().removeReview(session.getAdminUserId(), review.getReviewId(), reason));
    }

    @FXML
    private void handleRestoreReview() {
        applyStatusAction("Restore Review", "Add a reason for restoring this review.", (review, reason) ->
                appContext.getModerationService().restoreReview(session.getAdminUserId(), review.getReviewId(), reason));
    }

    @FXML
    private void handleEditReview() {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            statusLabel.setText("Select a review to edit.");
            return;
        }
        application.openReviewDialog("Admin Edit Review", selectedReview, true, draft -> {
            try {
                appContext.getModerationService().editReview(session.getAdminUserId(), selectedReview.getReviewId(), draft.getRating(),
                        draft.getComment(), "Admin content correction");
                refreshAll();
                statusLabel.setText("The selected review was edited and set back to ACTIVE.");
            } catch (BusinessException exception) {
                statusLabel.setText(exception.getMessage());
            }
        });
    }

    @FXML
    private void handleRefresh() {
        refreshAll();
    }

    @FXML
    private void handleClearFilters() {
        productFilterComboBox.getSelectionModel().clearSelection();
        statusFilterComboBox.getSelectionModel().clearSelection();
        searchField.clear();
        refreshAll();
    }

    @FXML
    private void handleLogout() {
        application.showLoginView();
    }

    private void refreshAll() {
        Product product = productFilterComboBox.getValue();
        ReviewStatus status = statusFilterComboBox.getValue();
        List<Review> reviews = appContext.getModerationService().getReviews(
                product == null ? null : product.getProductId(),
                status,
                searchField.getText());
        reviewTable.setItems(FXCollections.observableArrayList(reviews));

        List<ModerationAuditEntry> auditEntries = appContext.getModerationService().getAuditEntries();
        auditTable.setItems(FXCollections.observableArrayList(auditEntries));
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            statusLabel.setText("Showing " + reviewTable.getItems().size() + " reviews in the moderation queue.");
            return;
        }
        statusLabel.setText("Selected review " + selectedReview.getReviewId() + " for " + selectedReview.getProductName()
                + " currently has status " + selectedReview.getStatus().name() + ".");
    }

    private void applyStatusAction(String title, String prompt, ReviewAction reviewAction) {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            statusLabel.setText("Select a review first.");
            return;
        }
        Optional<String> reason = UiUtils.prompt(reviewTable.getScene().getWindow(), title, prompt, "");
        if (reason.isEmpty()) {
            return;
        }
        try {
            reviewAction.run(selectedReview, reason.get());
            refreshAll();
            statusLabel.setText(title + " completed successfully.");
        } catch (BusinessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface ReviewAction {
        void run(Review review, String reason);
    }
}
