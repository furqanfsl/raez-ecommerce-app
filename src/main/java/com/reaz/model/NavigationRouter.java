package com.reaz.model;

import Controllers.CustomerAdminDashboardController;
import com.reaz.customer.model.CustomerUser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Singleton scene manager / router.
 *
 * Boot sequence:
 *   1. MainApp.start() calls NavigationRouter.getInstance().init(stage)
 *   2. ProductHomepage.fxml is loaded as the initial scene root
 *   3. ProductHeaderController.initialize() calls setHeaderLoginListener(this::updateUserState)
 *
 * After login:
 *   ProductLoginModalController calls routeAfterLogin(user) which:
 *     - stores the current user
 *     - fires the header listener (customer stays on storefront with UI updated)
 *     - swaps the scene root for admin roles
 */
public class NavigationRouter {

    private static NavigationRouter instance;

    private Stage           primaryStage;
    private User            currentUser;
    private Consumer<User>  headerLoginListener;

    private NavigationRouter() {}

    public static NavigationRouter getInstance() {
        if (instance == null) instance = new NavigationRouter();
        return instance;
    }

    /** Called once from MainApp.start() before the first scene is shown. */
    public void init(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getStage()       { return primaryStage; }
    public User  getCurrentUser() { return currentUser;  }

    /**
     * ProductHeaderController registers here on every initialize() so that
     * login events update the header regardless of which storefront instance is active.
     */
    public void setHeaderLoginListener(Consumer<User> listener) {
        this.headerLoginListener = listener;
        // Immediately sync header with any already-logged-in user (e.g. back navigation)
        if (listener != null) listener.accept(currentUser);
    }

    // ── PUBLIC NAVIGATION API ──────────────────────────────────────────────

    /**
     * Called by ProductLoginModalController after successful authentication.
     * Evaluates the user's role and routes accordingly.
     */
    public void routeAfterLogin(User user) {
        this.currentUser = user;

        // Always notify the header (handles the customer "stay on storefront" case)
        if (headerLoginListener != null) headerLoginListener.accept(user);

        String role = user.roleName != null ? user.roleName : "";
        switch (role) {
            case "product_admin", "super_admin" -> navigateTo("/fxml/ProductAdminDashboard.fxml");
            case "customer_admin"               -> navigateToCustomerAdmin(user);
            // "customer" and any unknown role: remain on storefront — header updated above
        }
    }

    /**
     * Clears session and returns to the guest storefront.
     * The new ProductHeaderController will re-register and pick up null user (guest state).
     */
    public void logout() {
        currentUser        = null;
        headerLoginListener = null;
        navigateTo("/fxml/ProductHomepage.fxml");
    }

    /**
     * Swaps the primary stage's scene root.  Safe to call from any controller.
     */
    public void navigateTo(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("NavigationRouter.navigateTo failed: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────

    private void navigateToCustomerAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerAdminDashboard.fxml"));
            Parent view = loader.load();
            CustomerAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(bridgeToCustomerUser(user));
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("NavigationRouter: failed to load CustomerAdminDashboard");
            e.printStackTrace();
        }
    }

    /** Converts com.reaz.model.User → com.reaz.customer.model.CustomerUser. */
    private CustomerUser bridgeToCustomerUser(User u) {
        CustomerUser cu = new CustomerUser();
        cu.setId(u.userID);
        String name = ((u.firstName != null ? u.firstName : "") +
                       (u.lastName  != null ? " " + u.lastName.trim() : "")).trim();
        cu.setName(name.isEmpty() ? u.email : name);
        cu.setEmail(u.email);
        cu.setRole(u.roleName);
        cu.setStatus(u.isActive == 1 ? "ACTIVE" : "INACTIVE");
        return cu;
    }
}
