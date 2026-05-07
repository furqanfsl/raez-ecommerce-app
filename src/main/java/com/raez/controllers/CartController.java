package com.raez.controllers;

import com.raez.model.CartManager;
import com.raez.model.NavigationRouter;
import com.raez.model.User;
import com.raez.orders.dao.OrderDAO;
import com.raez.util.Validators;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

public class CartController {

    @FXML private VBox      itemsBox;
    @FXML private Label     emptyLabel;
    @FXML private StackPane emptyLabelHost;
    @FXML private Label     subtotalLabel;
    @FXML private Label     totalLabel;
    @FXML private Label     loginStatusLabel;
    @FXML private Label     statusLabel;
    @FXML private Button    checkoutBtn;
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

        // Boundary validation up-front, on the FX thread, before going off-thread.
        try {
            for (var item : cartManager.getItems().values()) {
                Validators.positiveInt(item.quantity, "Quantity for '" + item.productName + "'");
                Validators.positive(item.price, "Price for '" + item.productName + "'");
            }
        } catch (Exception e) {
            setStatus("Checkout failed: " + e.getMessage(), false);
            return;
        }

        String addr = addressField.getText().trim();
        if (addr.isEmpty()) addr = user.email;
        final String address = addr;
        final int userId = user.userID;
        final Map<Integer, CartManager.CartItem> snapshot = Map.copyOf(cartManager.getItems());

        checkoutBtn.setDisable(true);
        setStatus("Placing order…", true);

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                int customerId = orderDAO.getCustomerIdByUserId(userId);
                if (customerId < 0) throw new IllegalStateException("Customer record not found.");
                return orderDAO.createOrder(customerId, snapshot, address);
            }
        };
        task.setOnSucceeded(ev -> {
            int orderId = task.getValue();
            cartManager.clear();
            refreshCart();
            setStatus("Order #" + orderId + " placed! Status: Processing.\n" +
                      "The warehouse will pack it shortly.", true);
        });
        task.setOnFailed(ev -> {
            checkoutBtn.setDisable(cartManager.isEmpty());
            Throwable t = task.getException();
            setStatus("Checkout failed: " + (t == null ? "unknown" : t.getMessage()), false);
        });
        Thread thread = new Thread(task, "checkout-" + userId);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshCart() {
        itemsBox.getChildren().clear();
        Map<Integer, CartManager.CartItem> items = cartManager.getItems();

        boolean empty = items.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        if (emptyLabelHost != null) {
            emptyLabelHost.setVisible(empty);
            emptyLabelHost.setManaged(empty);
        }
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
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-background-color: rgba(13,21,32,0.78);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-radius: 16; -fx-border-width: 0.5;" +
            "-fx-padding: 16 18 16 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.2, 0, 4);"
        );

        // Thumbnail-style square chip with first letter — quick visual marker
        Label thumb = new Label(item.productName != null && !item.productName.isEmpty()
            ? String.valueOf(Character.toUpperCase(item.productName.charAt(0))) : "?");
        thumb.setStyle(
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 18; -fx-font-weight: 900;" +
            "-fx-text-fill: #06121f;" +
            "-fx-min-width: 48; -fx-min-height: 48;" +
            "-fx-max-width: 48; -fx-max-height: 48;" +
            "-fx-alignment: center;" +
            "-fx-background-color: linear-gradient(to bottom right, #5eead4, #38bdf8);" +
            "-fx-background-radius: 12;");

        VBox info = new VBox(2);
        Label name = new Label(item.productName);
        name.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
        Label unit = new Label(String.format("£%.2f each", item.price));
        unit.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.55);");
        info.getChildren().addAll(name, unit);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label qty = new Label("× " + item.quantity);
        qty.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.75);" +
            "-fx-padding: 4 12 4 12;" +
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 14; -fx-border-width: 0.5;");

        Label price = new Label(String.format("£%.2f", item.price * item.quantity));
        price.setStyle(
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 16; -fx-font-weight: 900; -fx-text-fill: #5eead4;");

        Button remove = new Button("✕");
        remove.setStyle(
            "-fx-background-color: rgba(248,113,113,0.10);" +
            "-fx-text-fill: #fca5a5;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-min-width: 32; -fx-min-height: 32;" +
            "-fx-max-width: 32; -fx-max-height: 32;" +
            "-fx-padding: 0;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(248,113,113,0.30);" +
            "-fx-border-radius: 16; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;");
        remove.setOnAction(e -> {
            cartManager.removeItem(item.productId);
            refreshCart();
        });

        row.getChildren().addAll(thumb, info, qty, price, remove);
        return row;
    }

    private void updateLoginStatus() {
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user != null && "customer".equals(user.roleName)) {
            loginStatusLabel.setText("✓  Signed in as " + user.firstName);
            loginStatusLabel.setStyle(
                "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
                "-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #5eead4;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-background-color: rgba(94,234,212,0.16);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(94,234,212,0.35);" +
                "-fx-border-radius: 18; -fx-border-width: 0.5;");
        } else {
            loginStatusLabel.setText("Guest · login required at checkout");
            loginStatusLabel.setStyle(
                "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
                "-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #fcd34d;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-background-color: rgba(252,211,77,0.14);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(252,211,77,0.35);" +
                "-fx-border-radius: 18; -fx-border-width: 0.5;");
        }
    }

    private void setStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (success ? "#5eead4" : "#fca5a5") + ";");
    }
}
