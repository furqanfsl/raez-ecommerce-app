package Controllers;

import com.reaz.model.CartManager;
import com.reaz.model.FavouritesManager;
import com.reaz.model.NavigationRouter;
import com.reaz.model.Product;
import com.reaz.model.User;
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
    @FXML private MenuButton menuBtn;
    @FXML private Button     adminBtn;
    @FXML private Label      cartBadge;
    @FXML private MenuButton userMenuBtn;
    @FXML private MenuItem   loginMenuItem;
    @FXML private MenuItem   logoutMenuItem;
    @FXML private Button     heartBtn;
    @FXML private Label      heartBadge;

    private final FavouritesManager favManager  = FavouritesManager.getInstance();
    private final CartManager       cartManager = CartManager.getInstance();
    private Popup favouritesPopup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (logoutMenuItem != null) logoutMenuItem.setVisible(false);
        if (adminBtn != null) { adminBtn.setVisible(false); adminBtn.setManaged(false); }

        favManager.addListener(products -> Platform.runLater(() -> updateFavBadge(products)));
        updateFavBadge(favManager.getAll());

        cartManager.addListener(items -> Platform.runLater(() -> updateCartBadge(items)));

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
        if (heartBtn != null)
            heartBtn.setText(count > 0 ? "♥" : "♡");
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
            "-fx-background-color: white;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        box.setMinWidth(280);
        box.setMaxWidth(320);

        Label title = new Label("♥  My Favourites (" + favs.size() + ")");
        title.setStyle(
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #111827;" +
            "-fx-padding: 12 16 12 16; -fx-border-color: #e5e7eb;" +
            "-fx-border-width: 0 0 1 0;"
        );
        title.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(title);

        for (Product p : favs) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setStyle("-fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;");

            Label nameLabel = new Label(p.name);
            nameLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #111827;");
            nameLabel.setMaxWidth(180);
            nameLabel.setWrapText(false);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label priceLabel = new Label(String.format("£%.2f", p.price));
            priceLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");

            Button removeBtn = new Button("✕");
            removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: #9ca3af; -fx-cursor: hand; -fx-font-size: 11;" +
                "-fx-padding: 2 4 2 4;"
            );
            removeBtn.setOnAction(e -> {
                favManager.remove(p.productID);
                if (favouritesPopup != null) favouritesPopup.hide();
                handleHeartClick();
            });

            row.getChildren().addAll(nameLabel, priceLabel, removeBtn);

            row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: #f9fafb; -fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;"));
            row.setOnMouseExited(e -> row.setStyle(
                "-fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;"));

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

    @FXML
    private void handleCartClick() {
        navigateTo("/fxml/Cart.fxml");
    }

    @FXML
    private void handleOpenLogin() {
        ProductLoginModalLauncher.show(null);
    }

    @FXML
    private void handleLogout() {
        NavigationRouter.getInstance().logout();
        // Scene root is replaced; this controller instance is discarded
    }

    private void updateUserState(User user) {
        if (user == null) { clearUserState(); return; }
        if (loginMenuItem  != null) loginMenuItem.setVisible(false);
        if (logoutMenuItem != null) logoutMenuItem.setVisible(true);
        if (userMenuBtn    != null) userMenuBtn.setText(user.firstName);
        if (user.isAdmin() && adminBtn != null) {
            adminBtn.setVisible(true);
            adminBtn.setManaged(true);
        }
    }

    private void clearUserState() {
        if (loginMenuItem  != null) loginMenuItem.setVisible(true);
        if (logoutMenuItem != null) logoutMenuItem.setVisible(false);
        if (adminBtn       != null) { adminBtn.setVisible(false); adminBtn.setManaged(false); }
        if (userMenuBtn    != null) userMenuBtn.setText("👤");
    }

    @FXML
    private void handleNavigateAdmin() {
        navigateTo("/fxml/ProductAdminDashboard.fxml");
    }

    @FXML
    private void handleNavigateHome() {
        navigateTo("/fxml/ProductHomepage.fxml");
    }

    @FXML
    private void handleLogoClick() {
        navigateTo("/fxml/ProductHomepage.fxml");
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
