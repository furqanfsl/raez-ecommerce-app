package com.raez.model;

import com.raez.controllers.CustomerAdminDashboardController;
import com.raez.controllers.CollectionPageController;
import com.raez.controllers.CustomerDashboardController;
import com.raez.controllers.ProductDetailController;
import com.raez.controllers.ProductRoutePageController;
import com.raez.controllers.SuperAdminDashboardController;
import com.raez.customer.model.CustomerUser;
import com.raez.dao.FavouritesDAO;
import com.raez.finance.model.FinanceUser;
import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.service.FinanceSessionManager;
import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.controller.AdminDashboardController;
import com.raez.reviews.model.AdminUser;
import com.raez.reviews.model.UserSession;
import com.raez.warehouse.Warehouse_StaffDashboardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationRouter {
    private static final Logger log = LoggerFactory.getLogger(NavigationRouter.class);


    private static NavigationRouter instance;

    private Stage           primaryStage;
    private User            currentUser;
    private Consumer<User>  headerLoginListener;

    private NavigationRouter() {}

    public static NavigationRouter getInstance() {
        if (instance == null) instance = new NavigationRouter();
        return instance;
    }

    public void init(Stage stage) { this.primaryStage = stage; }

    public Stage getStage()       { return primaryStage; }
    public User  getCurrentUser() { return currentUser;  }

    public void setHeaderLoginListener(Consumer<User> listener) {
        this.headerLoginListener = listener;
        if (listener != null) listener.accept(currentUser);
    }

    // ── PUBLIC NAVIGATION API ──────────────────────────────────────────────

    public void routeAfterLogin(User user) {
        this.currentUser = user;
        if (headerLoginListener != null) headerLoginListener.accept(user);

        String role = user.roleName != null ? user.roleName : "";

        // Load DB favourites for customer accounts
        if ("customer".equals(role)) {
            int customerId = new FavouritesDAO().getCustomerIdByUserId(user.userID);
            if (customerId >= 0) FavouritesManager.getInstance().loadForCustomer(customerId);
        }

        switch (role) {
            case "super_admin"                   -> navigateToSuperAdmin(user);
            case "product_admin"                 -> navigateTo("/fxml/ProductAdminDashboard.fxml");
            case "customer_admin"                -> navigateToCustomerAdmin(user);
            case "warehouse_admin"               -> navigateToWarehouseAdmin(user);
            case "delivery_admin"                -> navigateTo("/fxml/DeliveriesDashboard.fxml");
            case "orders_admin", "orders_user"   -> navigateTo("/fxml/OrdersDashboard.fxml");
            case "finance_admin", "finance_user" -> navigateToFinanceAdmin(user);
            case "reviews_admin"                 -> navigateToReviewsAdmin(user);
            // "customer" stays on storefront — header updated above
        }
    }

    public void logout() {
        FavouritesManager.getInstance().clearUser();
        currentUser         = null;
        headerLoginListener = null;
        navigateTo("/fxml/ProductHomepage.fxml");
    }

    public void navigateTo(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter.navigateTo failed: " + fxmlPath);
            log.error("Error", e);
        }
    }

    public void navigateToProductDetail(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailPage.fxml"));
            Parent view = loader.load();
            ProductDetailController ctrl = loader.getController();
            ctrl.setProduct(product);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load ProductDetailPage");
            log.error("Error", e);
        }
    }

    public void navigateByPath(String path) {
        if (path == null || path.isBlank()) return;
        if ("/".equals(path) || "/home".equals(path)) {
            navigateTo("/fxml/ProductHomepage.fxml");
            return;
        }
        if (path.startsWith("/collections/")) {
            String slug = path.substring("/collections/".length());
            navigateToCollectionPage(slug);
            return;
        }
        if (path.startsWith("/products/")) {
            String idPart = path.substring("/products/".length());
            try {
                navigateToProductRoute(Integer.parseInt(idPart));
                return;
            } catch (NumberFormatException ignored) {
                log.error("{}", "NavigationRouter.navigateByPath invalid product id: " + idPart);
            }
        }
        log.error("{}", "NavigationRouter.navigateByPath unknown path: " + path);
    }

    public void navigateToCollectionPage(String slug) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CollectionPage.fxml"));
            Parent view = loader.load();
            CollectionPageController ctrl = loader.getController();
            ctrl.setCollectionSlug(slug);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load CollectionPage");
            log.error("Error", e);
        }
    }

    public void navigateToProductRoute(int productID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductRoutePage.fxml"));
            Parent view = loader.load();
            ProductRoutePageController ctrl = loader.getController();
            ctrl.setProductId(productID);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load ProductRoutePage");
            log.error("Error", e);
        }
    }

    public void navigateToCustomerDashboard() {
        if (currentUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerDashboard.fxml"));
            Parent view = loader.load();
            CustomerDashboardController ctrl = loader.getController();
            ctrl.setUser(bridgeToCustomerUser(currentUser));
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load CustomerDashboard");
            log.error("Error", e);
        }
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────

    private void navigateToSuperAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/SuperAdminDashboard.fxml"));
            Parent view = loader.load();
            SuperAdminDashboardController ctrl = loader.getController();
            ctrl.setCurrentUser(user);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load SuperAdminDashboard");
            log.error("Error", e);
        }
    }

    private void navigateToWarehouseAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/WarehouseStaffDashboard.fxml"));
            Parent view = loader.load();
            Warehouse_StaffDashboardController ctrl = loader.getController();
            ctrl.setCurrentUserID(user.userID);
            ctrl.setOnLogout(this::logout);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load WarehouseStaffDashboard");
            log.error("Error", e);
        }
    }

    private void navigateToCustomerAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerAdminDashboard.fxml"));
            Parent view = loader.load();
            CustomerAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(bridgeToCustomerUser(user));
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load CustomerAdminDashboard");
            log.error("Error", e);
        }
    }

    private void navigateToFinanceAdmin(User user) {
        try {
            FinanceUserRole role = "finance_admin".equals(user.roleName)
                ? FinanceUserRole.ADMIN : FinanceUserRole.FINANCE_USER;
            FinanceUser financeUser = new FinanceUser(
                user.userID, user.email,
                user.username != null ? user.username : user.email,
                null, role,
                user.firstName, user.lastName,
                true, null
            );
            FinanceSessionManager.startSession(financeUser);
            FinanceSessionManager.setOnTimeoutCallback(this::logout);
            navigateTo("/com/raez/finance/view/FinanceMainLayout.fxml");
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load FinanceMainLayout");
            log.error("Error", e);
        }
    }

    private void navigateToReviewsAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews-admin-dashboard.fxml"));
            Parent view = loader.load();
            AdminDashboardController ctrl = loader.getController();
            String name = ((user.firstName != null ? user.firstName : "") + " " +
                           (user.lastName  != null ? user.lastName  : "")).trim();
            AdminUser adminUser = new AdminUser(user.userID,
                user.username != null ? user.username : user.email,
                name.isEmpty() ? user.email : name, true);
            UserSession session = UserSession.admin(adminUser);
            ReviewsApplication reviewsApp = new ReviewsApplication(primaryStage.getScene().getWindow());
            ctrl.init(reviewsApp, AppContext.getInstance(), session);
            primaryStage.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "NavigationRouter: failed to load reviews-admin-dashboard");
            log.error("Error", e);
        }
    }

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
