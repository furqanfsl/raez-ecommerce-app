package com.raez.reviews.app;

import java.util.function.Consumer;

import com.raez.model.NavigationRouter;
import com.raez.reviews.controller.AdminDashboardController;
import com.raez.reviews.controller.CustomerDashboardController;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewDraft;
import com.raez.reviews.model.UserSession;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.raez.reviews.controller.ReviewFormController;

/**
 * Adapter used by Reviews controllers — replaces the standalone JavaFX Application.
 * Provides dialog helpers and routes logout back through the master NavigationRouter.
 */
public class ReviewsApplication {

    private final Window ownerWindow;

    public ReviewsApplication(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    /** Opens the review form as a modal dialog. */
    public void openReviewDialog(String title, Review existingReview, boolean adminMode, Consumer<ReviewDraft> onSave) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews-review-form.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(ownerWindow);
            dialog.setTitle(title);
            dialog.setScene(new Scene(root));
            ReviewFormController ctrl = loader.getController();
            ctrl.init(dialog, title, existingReview, adminMode, onSave);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("[ReviewsApplication] openReviewDialog failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Routes logout back to the master storefront. */
    public void showLoginView() {
        javafx.application.Platform.runLater(() ->
            NavigationRouter.getInstance().logout());
    }

    public void showAdminDashboard(UserSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews-admin-dashboard.fxml"));
            Parent view = loader.load();
            AdminDashboardController ctrl = loader.getController();
            ctrl.init(this, AppContext.getInstance(), session);
            NavigationRouter.getInstance().getStage().getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("[ReviewsApplication] showAdminDashboard failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showCustomerDashboard(UserSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews-customer-dashboard.fxml"));
            Parent view = loader.load();
            CustomerDashboardController ctrl = loader.getController();
            ctrl.init(this, AppContext.getInstance(), session);
            NavigationRouter.getInstance().getStage().getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("[ReviewsApplication] showCustomerDashboard failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
