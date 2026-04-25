package com.raez.controllers;

import com.raez.model.CartManager;
import com.raez.model.FavouritesManager;
import com.raez.model.NavigationRouter;
import com.raez.model.Product;
import com.raez.model.User;
import com.raez.reviews.app.AppContext;
import com.raez.reviews.app.ReviewsApplication;
import com.raez.reviews.model.Customer;
import com.raez.reviews.model.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProductHeaderController implements Initializable {

    @FXML private Button     logoBtn;
    @FXML private TextField  searchField;
    @FXML private MenuButton collectionsMenu;
    @FXML private Button     adminBtn;
    @FXML private Label      cartBadge;
    @FXML private MenuButton userMenuBtn;
    @FXML private MenuItem   loginMenuItem;
    @FXML private MenuItem   manageAccountMenuItem;
    @FXML private MenuItem   myReviewsMenuItem;
    @FXML private MenuItem   logoutMenuItem;
    @FXML private SeparatorMenuItem logoutSeparator;
    @FXML private Button     heartBtn;
    @FXML private Label      heartBadge;

    private User currentUser;

    private final FavouritesManager favManager  = FavouritesManager.getInstance();
    private final CartManager       cartManager = CartManager.getInstance();
    private Popup favouritesPopup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (logoutMenuItem    != null) logoutMenuItem.setVisible(false);
        if (logoutSeparator   != null) logoutSeparator.setVisible(false);
        if (adminBtn          != null) { adminBtn.setVisible(false); adminBtn.setManaged(false); }
        if (userMenuBtn       != null) {
            userMenuBtn.setText("Login");
            userMenuBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-padding: 0;"
            );
        }
        if (collectionsMenu != null) {
            collectionsMenu.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-padding: 0;"
            );
        }

        favManager.addListener(products -> Platform.runLater(() -> updateFavBadge(products)));
        updateFavBadge(favManager.getAll());

        cartManager.addListener(items -> Platform.runLater(() -> updateCartBadge(items)));
        updateCartBadge(cartManager.getItems());

        // Wire search to ProductListController pending search
        if (searchField != null) {
            searchField.setOnAction(e -> handleSearch());
        }

        NavigationRouter.getInstance().setHeaderLoginListener(this::updateUserState);
    }

    private void updateCartBadge(java.util.Map<Integer, CartManager.CartItem> items) {
        if (cartBadge == null) return;
        int count = items.values().stream().mapToInt(i -> i.quantity).sum();
        cartBadge.setText(String.valueOf(count));
        cartBadge.setVisible(count > 0);
        cartBadge.setManaged(count > 0);
    }

    private void updateFavBadge(List<Product> products) {
        if (heartBadge == null) return;
        int count = products.size();
        heartBadge.setText(String.valueOf(count));
        heartBadge.setVisible(count > 0);
        heartBadge.setManaged(count > 0);
        if (heartBtn != null) heartBtn.setText(count > 0 ? "♥" : "♡");
    }

    @FXML
    private void handleHeartClick() {
        List<Product> favs = favManager.getAll();

        if (favs.isEmpty()) {
            if (heartBtn == null) return;
            Tooltip tip = new Tooltip("No favourites yet — click ♡ on any product");
            tip.setStyle("-fx-font-size: 12; -fx-padding: 8;");
            Tooltip.install(heartBtn, tip);
            tip.show(heartBtn,
                heartBtn.localToScreen(0, heartBtn.getHeight()).getX(),
                heartBtn.localToScreen(0, heartBtn.getHeight()).getY());
            return;
        }

        VBox box = new VBox(0);
        box.setStyle(
            "-fx-background-color: white; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        box.setMinWidth(280);
        box.setMaxWidth(320);

        Label title = new Label("♥  My Favourites (" + favs.size() + ")");
        title.setStyle(
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #111827;" +
            "-fx-padding: 12 16 12 16; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;"
        );
        title.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(title);

        for (Product p : favs) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setStyle("-fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;");
            row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: #f9fafb; -fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;"));
            row.setOnMouseExited(e -> row.setStyle(
                "-fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;"));

            Label nameLabel = new Label(p.name);
            nameLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #111827;");
            nameLabel.setMaxWidth(180);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label priceLabel = new Label(String.format("£%.2f", p.price));
            priceLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");

            Button removeBtn = new Button("✕");
            removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: #9ca3af; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 2 4 2 4;"
            );
            removeBtn.setOnAction(e -> {
                favManager.remove(p.productID);
                if (favouritesPopup != null) favouritesPopup.hide();
                handleHeartClick();
            });

            row.getChildren().addAll(nameLabel, priceLabel, removeBtn);
            box.getChildren().add(row);
        }

        Button clearBtn = new Button("Clear All Favourites");
        clearBtn.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent;" +
            "-fx-text-fill: #dc2626; -fx-cursor: hand; -fx-font-size: 12;" +
            "-fx-padding: 10 16 10 16;"
        );
        clearBtn.setOnAction(e -> {
            new ArrayList<>(favManager.getAll()).forEach(p -> favManager.remove(p.productID));
            if (favouritesPopup != null) favouritesPopup.hide();
        });
        box.getChildren().add(clearBtn);

        if (favouritesPopup != null) favouritesPopup.hide();
        favouritesPopup = new Popup();
        favouritesPopup.getContent().add(box);
        favouritesPopup.setAutoHide(true);

        if (heartBtn != null && heartBtn.getScene() != null) {
            javafx.geometry.Point2D point = heartBtn.localToScreen(0, heartBtn.getHeight() + 4);
            favouritesPopup.show(heartBtn.getScene().getWindow(), point.getX(), point.getY());
        }
    }

    @FXML private void handleCartClick()  { navigateTo("/fxml/Cart.fxml"); }
    @FXML private void handleOpenLogin()  { ProductLoginModalLauncher.show(null); }
    @FXML private void handleLogout()     { NavigationRouter.getInstance().logout(); }

    @FXML
    private void handleManageAccount() {
        NavigationRouter.getInstance().navigateToCustomerDashboard();
    }

    // ── Collection filters ─────────────────────────────────────────────────

    @FXML private void handleFilterAll()                    { navigateTo("/fxml/ProductHomepage.fxml"); }
    @FXML private void handleCollectionApexAutomata()       { NavigationRouter.getInstance().navigateByPath("/collections/the-apex-series"); }
    @FXML private void handleCollectionSentinelForce()      { NavigationRouter.getInstance().navigateByPath("/collections/the-ledger-series"); }
    @FXML private void handleCollectionNovaMind()           { NavigationRouter.getInstance().navigateByPath("/collections/the-velocity-series"); }
    @FXML private void handleCollectionTerraCore()          { NavigationRouter.getInstance().navigateByPath("/collections/the-sentinel-series"); }

    // ── Top-level nav ──────────────────────────────────────────────────────

    @FXML private void handleNavRobots()      { filterByCategory("Main Robot"); }
    @FXML private void handleNavMiniRobots()  { filterByCategory("Mini Robot"); }
    @FXML private void handleNavAccessories() { filterByCategory("Accessory"); }
    @FXML private void handleNavServices()    { filterByCategory("Service"); }

    private void filterByCategory(String category) {
        ProductListController.setPendingCategory(category);
        navigateTo("/fxml/ProductListPage.fxml");
    }

    // ── Search ─────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        if (searchField == null) return;
        String query = searchField.getText().trim();
        ProductListController.setPendingSearch(query);
        navigateTo("/fxml/ProductHomepage.fxml");
    }

    // ── User state ─────────────────────────────────────────────────────────

    private void updateUserState(User user) {
        this.currentUser = user;
        if (user == null) { clearUserState(); return; }

        if (loginMenuItem          != null) loginMenuItem.setVisible(false);
        if (logoutMenuItem         != null) logoutMenuItem.setVisible(true);
        if (logoutSeparator        != null) logoutSeparator.setVisible(true);
        if (userMenuBtn != null) {
            String name = user.firstName != null && !user.firstName.isBlank() ? user.firstName : "Account";
            userMenuBtn.setText("👤  " + name);
            userMenuBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-size: 13; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0;"
            );
        }

        boolean isCustomer = "customer".equals(user.roleName);
        if (manageAccountMenuItem  != null) manageAccountMenuItem.setVisible(isCustomer);
        if (myReviewsMenuItem      != null) myReviewsMenuItem.setVisible(isCustomer);
        if (user.isAdmin() && adminBtn != null) {
            adminBtn.setVisible(true);
            adminBtn.setManaged(true);
        }
    }

    private void clearUserState() {
        this.currentUser = null;
        if (loginMenuItem         != null) loginMenuItem.setVisible(true);
        if (logoutMenuItem        != null) logoutMenuItem.setVisible(false);
        if (logoutSeparator       != null) logoutSeparator.setVisible(false);
        if (manageAccountMenuItem != null) manageAccountMenuItem.setVisible(false);
        if (myReviewsMenuItem     != null) myReviewsMenuItem.setVisible(false);
        if (adminBtn              != null) { adminBtn.setVisible(false); adminBtn.setManaged(false); }
        if (userMenuBtn != null) {
            userMenuBtn.setText("Login");
            userMenuBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-size: 13; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0;"
            );
        }
    }

    @FXML
    private void handleMyReviews() {
        if (currentUser == null) { handleOpenLogin(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews-customer-dashboard.fxml"));
            Parent view = loader.load();
            com.raez.reviews.controller.CustomerDashboardController ctrl = loader.getController();
            String displayName = ((currentUser.firstName != null ? currentUser.firstName : "") + " " +
                                  (currentUser.lastName  != null ? currentUser.lastName  : "")).trim();
            if (displayName.isEmpty()) displayName = currentUser.email;
            Customer cu = new Customer(currentUser.userID, displayName, currentUser.email, true);
            UserSession session = UserSession.customer(cu);
            ReviewsApplication reviewsApp = new ReviewsApplication(logoBtn.getScene().getWindow());
            ctrl.init(reviewsApp, AppContext.getInstance(), session);
            logoBtn.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("ProductHeaderController: failed to load reviews-customer-dashboard");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateAdmin() {
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user != null && "super_admin".equals(user.roleName)) {
            NavigationRouter.getInstance().routeAfterLogin(user);
        } else {
            navigateTo("/fxml/ProductAdminDashboard.fxml");
        }
    }

    @FXML private void handleNavigateHome() { routeHome(); }
    @FXML private void handleLogoClick()    { routeHome(); }

    private void routeHome() {
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user != null && "super_admin".equals(user.roleName)) {
            NavigationRouter.getInstance().routeAfterLogin(user);
        } else {
            navigateTo("/fxml/ProductHomepage.fxml");
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            logoBtn.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("Navigation failed for " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
