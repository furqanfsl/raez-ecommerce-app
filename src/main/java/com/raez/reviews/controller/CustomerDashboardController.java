package com.raez.reviews.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.UserSession;
import com.raez.reviews.util.UiUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * My Reviews — a clean list of the current customer's own reviews.
 * Filterable by time range (All time, last 30 days, last 90 days).
 */
public class CustomerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label reviewCountLabel;
    @FXML private ComboBox<String> timeFilterComboBox;
    @FXML private VBox reviewsList;

    private ReviewsApplication application;
    private AppContext appContext;
    private UserSession session;

    private static final String ALL_TIME = "All time";
    private static final String LAST_30  = "Last 30 days";
    private static final String LAST_90  = "Last 90 days";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy");

    @FXML
    private void initialize() {
        timeFilterComboBox.setItems(FXCollections.observableArrayList(ALL_TIME, LAST_30, LAST_90));
        timeFilterComboBox.setValue(ALL_TIME);
        timeFilterComboBox.valueProperty().addListener((o, a, b) -> refresh());
    }

    public void init(ReviewsApplication application, AppContext appContext, UserSession session) {
        this.application = application;
        this.appContext = appContext;
        this.session = session;
        welcomeLabel.setText("Signed in as " + session.getDisplayName());
        refresh();
    }

    @FXML
    private void handleLogout() {
        application.showLoginView();
    }

    @FXML
    private void handleBackToShop() {
        try {
            javafx.scene.Parent view = javafx.fxml.FXMLLoader.load(
                    getClass().getResource("/fxml/ProductHomepage.fxml"));
            reviewsList.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("CustomerDashboardController: back to shop failed: " + e.getMessage());
        }
    }

    private void refresh() {
        List<Review> allReviews = appContext.getReviewService()
                .getReviewsByCustomer(session.getCustomerId());
        List<Review> filtered = applyTimeFilter(allReviews);
        reviewCountLabel.setText(filtered.size() + (filtered.size() == 1 ? " review" : " reviews"));

        reviewsList.getChildren().clear();
        if (filtered.isEmpty()) {
            reviewsList.getChildren().add(buildEmptyState(allReviews.isEmpty()));
            return;
        }
        for (Review r : filtered) {
            reviewsList.getChildren().add(buildReviewCard(r));
        }
    }

    private List<Review> applyTimeFilter(List<Review> reviews) {
        String mode = timeFilterComboBox.getValue();
        if (mode == null || ALL_TIME.equals(mode)) return reviews;
        long days = LAST_30.equals(mode) ? 30 : 90;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return reviews.stream()
                .filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().isAfter(cutoff))
                .toList();
    }

    private VBox buildEmptyState(boolean noReviewsAtAll) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 48 0; -fx-background-color: #f9fafb;" +
                     "-fx-background-radius: 10; -fx-border-color: #e5e7eb;" +
                     "-fx-border-radius: 10; -fx-border-width: 1;");
        Label icon = new Label("★");
        icon.setStyle("-fx-font-size: 36; -fx-text-fill: #d1d5db;");
        Label title = new Label(noReviewsAtAll
                ? "You haven't written any reviews yet."
                : "No reviews in this time range.");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #374151;");
        Label body = new Label(noReviewsAtAll
                ? "Once you buy a product and it's delivered, you can leave a review here."
                : "Try a wider time range.");
        body.setStyle("-fx-font-size: 13; -fx-text-fill: #6b7280;");
        box.getChildren().addAll(icon, title, body);
        return box;
    }

    private VBox buildReviewCard(Review r) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 18 22;" +
                      "-fx-border-color: #e5e7eb; -fx-border-radius: 10;" +
                      "-fx-background-radius: 10;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0.1, 0, 1);");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label product = new Label(r.getProductName() != null ? r.getProductName() : "Product #" + r.getProductId());
        product.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label stars = new Label(renderStars(r.getRating()));
        stars.setStyle("-fx-font-size: 14; -fx-text-fill: #f59e0b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(r.getUpdatedAt() != null ? r.getUpdatedAt().format(DATE_FMT) : "");
        date.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        topRow.getChildren().addAll(product, stars, spacer, date);

        Label comment = new Label(r.getComment() == null || r.getComment().isBlank()
                ? "(no comment)" : r.getComment());
        comment.setWrapText(true);
        comment.setStyle("-fx-font-size: 13; -fx-text-fill: #374151; -fx-line-spacing: 2;");

        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label helpful = new Label("👍 " + r.getHelpfulCount());
        helpful.setStyle("-fx-font-size: 12; -fx-text-fill: #16a34a;");
        Label unhelpful = new Label("👎 " + r.getUnhelpfulCount());
        unhelpful.setStyle("-fx-font-size: 12; -fx-text-fill: #9ca3af;");
        Label status = new Label(r.getStatus() != null ? r.getStatus().name() : "");
        status.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280; -fx-background-color: #f3f4f6;" +
                        "-fx-padding: 2 8; -fx-background-radius: 10;");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;" +
                         "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12;" +
                         "-fx-padding: 6 12; -fx-cursor: hand;");
        boolean editable = appContext.getReviewService().isEditableByCustomer(r, session.getCustomerId());
        editBtn.setDisable(!editable);
        editBtn.setOnAction(e -> openEditor(r));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: white; -fx-border-color: #fecaca;" +
                           "-fx-text-fill: #dc2626; -fx-border-radius: 6;" +
                           "-fx-background-radius: 6; -fx-font-size: 12;" +
                           "-fx-padding: 6 12; -fx-cursor: hand;");
        deleteBtn.setDisable(!editable);
        deleteBtn.setOnAction(e -> deleteReview(r));

        meta.getChildren().addAll(helpful, unhelpful, status, metaSpacer, editBtn, deleteBtn);

        Duration remaining = appContext.getReviewService().getRemainingEditDuration(r, session.getCustomerId());
        Label window = new Label(formatEditWindow(remaining));
        window.setStyle("-fx-font-size: 11; -fx-text-fill: #9ca3af;");

        card.getChildren().addAll(topRow, comment, meta, window);
        return card;
    }

    private String renderStars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= rating ? '★' : '☆');
        sb.append("  ").append(rating).append("/5");
        return sb.toString();
    }

    private String formatEditWindow(Duration remaining) {
        if (remaining == null || remaining.isZero() || remaining.isNegative()) {
            return "Edit window closed";
        }
        long minutes = remaining.toMinutes();
        if (minutes >= 60) {
            long hours = minutes / 60;
            return "Editable for " + hours + "h " + (minutes % 60) + "m";
        }
        return "Editable for " + minutes + "m";
    }

    private void openEditor(Review review) {
        application.openReviewDialog("Edit Review", review, false, draft -> execute(
                () -> appContext.getReviewService().editReview(session.getCustomerId(), review.getReviewId(),
                        draft.getRating(), draft.getComment()),
                "Review updated", "Your review was updated.", "Unable to update review"));
    }

    private void deleteReview(Review review) {
        boolean confirmed = UiUtils.confirm(reviewsList.getScene().getWindow(), "Delete Review",
                "Delete this review? This cannot be undone.");
        if (!confirmed) return;
        execute(() -> appContext.getReviewService().deleteReview(session.getCustomerId(), review.getReviewId()),
                "Review deleted", "Your review has been removed.", "Unable to delete review");
    }

    private void execute(Runnable action, String successTitle, String successBody, String errorTitle) {
        try {
            action.run();
            refresh();
            UiUtils.showInfo(reviewsList.getScene().getWindow(), successTitle, successBody);
        } catch (BusinessException ex) {
            UiUtils.showError(reviewsList.getScene().getWindow(), errorTitle, ex.getMessage());
        }
    }

}
