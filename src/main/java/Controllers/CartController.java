package Controllers;

import com.reaz.model.CartManager;
import com.reaz.model.NavigationRouter;
import com.reaz.model.User;
import com.reaz.orders.dao.OrderDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

public class CartController {

    @FXML private VBox   itemsBox;
    @FXML private Label  emptyLabel;
    @FXML private Label  subtotalLabel;
    @FXML private Label  totalLabel;
    @FXML private Label  loginStatusLabel;
    @FXML private Label  statusLabel;
    @FXML private Button checkoutBtn;
    @FXML private TextField addressField;

    private final CartManager cartManager = CartManager.getInstance();
    private final OrderDAO    orderDAO    = new OrderDAO();

    @FXML
    public void initialize() {
        refreshCart();
        updateLoginStatus();
    }

    @FXML private void handleBack() {
        NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml");
    }

    @FXML private void handleClear() {
        cartManager.clear();
        refreshCart();
    }

    @FXML
    private void handleCheckout() {
        if (cartManager.isEmpty()) {
            setStatus("Your cart is empty.", false);
            return;
        }

        User user = NavigationRouter.getInstance().getCurrentUser();

        if (user == null || !"customer".equals(user.roleName)) {
            setStatus("Please log in to checkout.", false);
            ProductLoginModalLauncher.show(u -> Platform.runLater(() -> {
                updateLoginStatus();
                if (u != null && "customer".equals(u.roleName)) {
                    attemptCheckout();
                }
            }));
            return;
        }

        attemptCheckout();
    }

    private void attemptCheckout() {
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user == null) { setStatus("Not logged in.", false); return; }

        int customerId = orderDAO.getCustomerIdByUserId(user.userID);
        if (customerId < 0) {
            setStatus("Customer record not found. Please contact support.", false);
            return;
        }

        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            User u = NavigationRouter.getInstance().getCurrentUser();
            address = u.email;
        }

        try {
            int orderId = orderDAO.createOrder(customerId, cartManager.getItems(), address);
            cartManager.clear();
            refreshCart();
            setStatus("Order #" + orderId + " placed! Status: Processing.\n" +
                      "The warehouse will pack it shortly.", true);
        } catch (Exception e) {
            setStatus("Checkout failed: " + e.getMessage(), false);
        }
    }

    private void refreshCart() {
        itemsBox.getChildren().clear();
        Map<Integer, CartManager.CartItem> items = cartManager.getItems();

        boolean empty = items.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        checkoutBtn.setDisable(empty);

        double subtotal = 0;
        for (CartManager.CartItem item : items.values()) {
            subtotal += item.price * item.quantity;
            itemsBox.getChildren().add(buildRow(item));
        }

        subtotalLabel.setText(String.format("£%.2f", subtotal));
        totalLabel.setText(String.format("£%.2f", subtotal));
    }

    private HBox buildRow(CartManager.CartItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:white;-fx-border-color:#e5e7eb;-fx-border-radius:8;" +
                     "-fx-background-radius:8;-fx-padding:14 16;");

        Label name  = new Label(item.productName);
        name.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#111827;");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label qty = new Label("× " + item.quantity);
        qty.setStyle("-fx-font-size:13;-fx-text-fill:#6b7280;");

        Label price = new Label(String.format("£%.2f", item.price * item.quantity));
        price.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#111827;");

        Button remove = new Button("✕");
        remove.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-text-fill:#9ca3af;-fx-cursor:hand;-fx-font-size:12;");
        remove.setOnAction(e -> {
            cartManager.removeItem(item.productId);
            refreshCart();
        });

        row.getChildren().addAll(name, qty, price, remove);
        return row;
    }

    private void updateLoginStatus() {
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user != null && "customer".equals(user.roleName)) {
            loginStatusLabel.setText("Logged in as " + user.firstName);
            loginStatusLabel.setStyle("-fx-font-size:13;-fx-text-fill:#16a34a;");
        } else {
            loginStatusLabel.setText("Guest — login required at checkout");
            loginStatusLabel.setStyle("-fx-font-size:13;-fx-text-fill:#f59e0b;");
        }
    }

    private void setStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size:13;-fx-text-fill:" +
                (success ? "#16a34a" : "#dc2626") + ";");
    }
}
