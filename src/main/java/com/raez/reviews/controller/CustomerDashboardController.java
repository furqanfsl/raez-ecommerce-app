package com.raez.reviews.controller;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Product;
import com.raez.reviews.model.ProductReviewSummary;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewDraft;
import com.raez.reviews.model.ReviewSortOption;
import com.raez.reviews.model.UserSession;
import com.raez.reviews.model.VoteType;
import com.raez.reviews.util.UiUtils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class CustomerDashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Product> productComboBox;
    @FXML
    private ComboBox<ReviewSortOption> sortComboBox;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Label reviewCountLabel;
    @FXML
    private Label eligibilityLabel;
    @FXML
    private Label editWindowLabel;
    @FXML
    private TableView<Review> reviewTable;
    @FXML
    private TableColumn<Review, String> reviewerColumn;
    @FXML
    private TableColumn<Review, Number> ratingColumn;
    @FXML
    private TableColumn<Review, String> commentColumn;
    @FXML
    private TableColumn<Review, Number> helpfulColumn;
    @FXML
    private TableColumn<Review, Number> unhelpfulColumn;
    @FXML
    private TableColumn<Review, String> statusColumn;
    @FXML
    private TableColumn<Review, String> createdColumn;
    @FXML
    private BarChart<String, Number> ratingChart;
    @FXML
    private Button addReviewButton;
    @FXML
    private Button editReviewButton;
    @FXML
    private Button deleteReviewButton;
    @FXML
    private Button helpfulButton;
    @FXML
    private Button unhelpfulButton;

    private ReviewsApplication application;
    private AppContext appContext;
    private UserSession session;
    private Timeline countdownTimeline;

    @FXML
    private void initialize() {
        CustomerDashboardViewSupport.configureReviewTable(reviewTable, reviewerColumn, ratingColumn, commentColumn, helpfulColumn,
                unhelpfulColumn, statusColumn, createdColumn);
        configureSelectors();
        reviewTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateButtonState());
    }

    public void init(ReviewsApplication application, AppContext appContext, UserSession session) {
        this.application = application;
        this.appContext = appContext;
        this.session = session;
        welcomeLabel.setText("Logged in as " + session.getDisplayName());
        loadProducts();
        startCountdownTimer();
    }

    @FXML
    private void handleAddReview() {
        Product product = requireSelectedProduct();
        if (product == null) {
            return;
        }
        openReviewEditor("Add Review", null,
                draft -> appContext.getReviewService().createReview(session.getCustomerId(), product.getProductId(),
                        draft.getRating(), draft.getComment()),
                "Review saved", "Your review was saved successfully.", "Unable to save review");
    }

    @FXML
    private void handleEditReview() {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            return;
        }
        openReviewEditor("Edit Review", selectedReview,
                draft -> appContext.getReviewService().editReview(session.getCustomerId(), selectedReview.getReviewId(),
                        draft.getRating(), draft.getComment()),
                "Review updated", "Your review was updated successfully.", "Unable to update review");
    }

    @FXML
    private void handleDeleteReview() {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            return;
        }
        boolean confirmed = UiUtils.confirm(reviewTable.getScene().getWindow(), "Delete Review",
                "Delete the selected review from the customer view?");
        if (!confirmed) {
            return;
        }
        executeBusinessAction(
                () -> appContext.getReviewService().deleteReview(session.getCustomerId(), selectedReview.getReviewId()),
                "Review deleted", "The selected review has been archived.", "Unable to delete review");
    }

    @FXML
    private void handleHelpfulVote() {
        applyVote(VoteType.HELPFUL);
    }

    @FXML
    private void handleUnhelpfulVote() {
        applyVote(VoteType.UNHELPFUL);
    }

    @FXML
    private void handleRefresh() {
        refreshProductDetails();
    }

    @FXML
    private void handleLogout() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        application.showLoginView();
    }

    private void configureSelectors() {
        sortComboBox.setItems(FXCollections.observableArrayList(ReviewSortOption.values()));
        sortComboBox.getSelectionModel().select(ReviewSortOption.NEWEST);
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshProductDetails());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadProducts());
        productComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshProductDetails());
    }

    private Product requireSelectedProduct() {
        Product product = productComboBox.getValue();
        if (product == null) {
            UiUtils.showError(reviewTable.getScene().getWindow(), "No product selected", "Please choose a product first.");
        }
        return product;
    }

    private void openReviewEditor(String title, Review existingReview, ReviewDraftOperation operation,
            String successTitle, String successMessage, String errorTitle) {
        application.openReviewDialog(title, existingReview, false,
                draft -> executeBusinessAction(() -> operation.apply(draft), successTitle, successMessage, errorTitle));
    }

    private void executeBusinessAction(BusinessAction action, String successTitle, String successMessage, String errorTitle) {
        try {
            action.run();
            refreshProductDetails();
            UiUtils.showInfo(reviewTable.getScene().getWindow(), successTitle, successMessage);
        } catch (BusinessException exception) {
            UiUtils.showError(reviewTable.getScene().getWindow(), errorTitle, exception.getMessage());
        }
    }

    private void loadProducts() {
        List<Product> products = appContext.getReviewService().getProducts(searchField.getText());
        Product currentSelection = productComboBox.getValue();
        productComboBox.setItems(FXCollections.observableArrayList(products));
        restoreProductSelection(currentSelection);
    }

    private void restoreProductSelection(Product currentSelection) {
        // Preserve the current product when search filters or review actions reload the combo box.
        if (currentSelection != null) {
            productComboBox.getItems().stream()
                    .filter(product -> product.getProductId() == currentSelection.getProductId())
                    .findFirst()
                    .ifPresentOrElse(productComboBox.getSelectionModel()::select, this::selectFirstAvailableProduct);
            return;
        }
        selectFirstAvailableProduct();
    }

    private void selectFirstAvailableProduct() {
        if (!productComboBox.getItems().isEmpty()) {
            productComboBox.getSelectionModel().selectFirst();
            return;
        }
        productComboBox.getSelectionModel().clearSelection();
        clearProductDetails();
    }

    private void refreshProductDetails() {
        Product product = productComboBox.getValue();
        if (product == null) {
            clearProductDetails();
            return;
        }

        reviewTable.setItems(FXCollections.observableArrayList(
                appContext.getReviewService().getReviewsForProduct(product.getProductId(), getSelectedSortOption())));
        updateProductSummary(product.getProductId());
        updateButtonState();
    }

    private ReviewSortOption getSelectedSortOption() {
        return sortComboBox.getValue() == null ? ReviewSortOption.NEWEST : sortComboBox.getValue();
    }

    private void clearProductDetails() {
        CustomerDashboardViewSupport.clearProductDetails(reviewTable, averageRatingLabel, reviewCountLabel, eligibilityLabel,
                editWindowLabel, ratingChart);
        updateButtonState();
    }

    private void updateProductSummary(int productId) {
        ProductReviewSummary summary = appContext.getReviewService().getProductSummary(productId);
        CustomerDashboardViewSupport.updateSummaryLabels(summary, averageRatingLabel, reviewCountLabel);
        updateEligibilityLabel(productId);
        CustomerDashboardViewSupport.updateRatingChart(ratingChart, summary);
    }

    private void updateEligibilityLabel(int productId) {
        boolean purchased = appContext.getReviewService().hasPurchasedProduct(session.getCustomerId(), productId);
        Optional<Review> existingReview = appContext.getReviewService()
                .getCustomerReviewForProduct(session.getCustomerId(), productId);
        // The customer can only post one review per purchased product, so the message needs both checks.
        if (!purchased) {
            eligibilityLabel.setText("You cannot review this product until you have purchased it.");
        } else if (existingReview.isPresent()) {
            eligibilityLabel
                    .setText("You already submitted a review for this product. Select it to edit or delete if time remains.");
        } else {
            eligibilityLabel.setText("You are eligible to review this product.");
        }
    }

    private void updateButtonState() {
        Product currentProduct = productComboBox.getValue();
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        Optional<Review> ownReview = loadOwnReview(currentProduct);

        boolean canEditSelected = isEditableSelection(selectedReview);
        boolean canVoteSelected = canVoteOnReview(selectedReview);

        addReviewButton.setDisable(!canAddReview(currentProduct, ownReview));
        editReviewButton.setDisable(!canEditSelected);
        deleteReviewButton.setDisable(!canEditSelected);
        helpfulButton.setDisable(!canVoteSelected);
        unhelpfulButton.setDisable(!canVoteSelected);
        updateEditWindowLabel(selectedReview);
    }

    private Optional<Review> loadOwnReview(Product currentProduct) {
        if (currentProduct == null) {
            return Optional.empty();
        }
        return appContext.getReviewService().getCustomerReviewForProduct(session.getCustomerId(), currentProduct.getProductId());
    }

    private boolean canAddReview(Product currentProduct, Optional<Review> ownReview) {
        return currentProduct != null
                && appContext.getReviewService().hasPurchasedProduct(session.getCustomerId(), currentProduct.getProductId())
                && ownReview.isEmpty();
    }

    private boolean isEditableSelection(Review selectedReview) {
        return selectedReview != null
                && appContext.getReviewService().isEditableByCustomer(selectedReview, session.getCustomerId());
    }

    private boolean canVoteOnReview(Review selectedReview) {
        return selectedReview != null && selectedReview.getCustomerId() != session.getCustomerId();
    }

    private void updateEditWindowLabel(Review selectedReview) {
        if (selectedReview == null || selectedReview.getCustomerId() != session.getCustomerId()) {
            editWindowLabel.setText("Select your own review to view the edit timer.");
            return;
        }

        Duration remaining = appContext.getReviewService().getRemainingEditDuration(selectedReview, session.getCustomerId());
        editWindowLabel.setText(CustomerDashboardViewSupport.formatEditWindow(remaining));
    }

    private void applyVote(VoteType voteType) {
        Review selectedReview = reviewTable.getSelectionModel().getSelectedItem();
        if (selectedReview == null) {
            return;
        }
        executeBusinessAction(() -> appContext.getVoteService().addVote(session.getCustomerId(), selectedReview.getReviewId(), voteType),
                "Vote saved", "Your vote was recorded.", "Unable to save vote");
    }

    private void startCountdownTimer() {
        // Refresh once per second so the edit timer and button states stay in sync while the window is open.
        countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateButtonState()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    @FunctionalInterface
    private interface BusinessAction {
        void run();
    }

    @FunctionalInterface
    private interface ReviewDraftOperation {
        void apply(ReviewDraft draft);
    }
}
