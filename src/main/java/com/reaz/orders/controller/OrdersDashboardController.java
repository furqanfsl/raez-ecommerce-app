package com.reaz.orders.controller;

import com.reaz.model.NavigationRouter;
import com.reaz.orders.dao.OrderDAO;
import com.reaz.orders.model.Order;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.List;

public class OrdersDashboardController {

    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     statusFilter;
    @FXML private Label                countLabel;
    @FXML private Label                statsLabel;
    @FXML private TableView<Order>     ordersTable;
    @FXML private TableColumn<Order, Integer> idCol;
    @FXML private TableColumn<Order, String>  customerCol;
    @FXML private TableColumn<Order, String>  dateCol;
    @FXML private TableColumn<Order, Double>  totalCol;
    @FXML private TableColumn<Order, Integer> itemsCol;
    @FXML private TableColumn<Order, String>  statusCol;
    @FXML private TableColumn<Order, Void>    actionsCol;

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Order> allOrders = FXCollections.observableArrayList();
    private FilteredList<Order> filtered;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(c -> c.getValue().orderIdProperty().asObject());
        customerCol.setCellValueFactory(c -> c.getValue().customerProperty());
        dateCol.setCellValueFactory(c -> c.getValue().orderDateProperty());
        totalCol.setCellValueFactory(c -> c.getValue().totalProperty().asObject());
        itemsCol.setCellValueFactory(c -> c.getValue().itemCountProperty().asObject());
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());

        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("£%.2f", v));
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String colour = switch (v) {
                    case "Processing" -> "#f59e0b";
                    case "Picking"    -> "#3b82f6";
                    case "Shipped"    -> "#8b5cf6";
                    case "Delivered"  -> "#16a34a";
                    case "Cancelled"  -> "#dc2626";
                    default           -> "#6b7280";
                };
                setStyle("-fx-text-fill:" + colour + ";-fx-font-weight:bold;");
            }
        });

        wireActionsCol();

        statusFilter.getItems().setAll("All", "Processing", "Confirmed", "Picking",
                "Shipped", "Delivered", "Cancelled", "Refunded");
        statusFilter.setValue("All");

        filtered = new FilteredList<>(allOrders, p -> true);
        ordersTable.setItems(filtered);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        loadOrders();
    }

    @FXML private void handleRefresh() { loadOrders(); }
    @FXML private void handleLogout()  { NavigationRouter.getInstance().logout(); }

    private void loadOrders() {
        List<Order> list = orderDAO.getAllOrders();
        allOrders.setAll(list);
        applyFilter();

        long processing = list.stream().filter(o -> "Processing".equals(o.getStatus())).count();
        long delivered  = list.stream().filter(o -> "Delivered".equals(o.getStatus())).count();
        statsLabel.setText("Total: " + list.size() + "  |  Processing: " + processing +
                "  |  Delivered: " + delivered);
    }

    private void applyFilter() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String status = statusFilter.getValue();
        filtered.setPredicate(o -> {
            boolean statusOk = "All".equals(status) || status.equals(o.getStatus());
            boolean searchOk = search.isEmpty()
                    || String.valueOf(o.getOrderId()).contains(search)
                    || o.getCustomer().toLowerCase().contains(search);
            return statusOk && searchOk;
        });
        countLabel.setText(filtered.size() + " orders");
    }

    private void wireActionsCol() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn   = new Button("Cancel");
            private final Button confirmBtn  = new Button("Confirm");
            private final HBox   box         = new HBox(6, confirmBtn, cancelBtn);

            {
                confirmBtn.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:5;-fx-cursor:hand;");
                cancelBtn.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:5;-fx-cursor:hand;");

                confirmBtn.setOnAction(e -> {
                    Order o = getTableRow().getItem();
                    if (o == null) return;
                    String next = nextStatus(o.getStatus());
                    if (next == null) return;
                    if (orderDAO.updateOrderStatus(o.getOrderId(), next)) loadOrders();
                });
                cancelBtn.setOnAction(e -> {
                    Order o = getTableRow().getItem();
                    if (o == null) return;
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Cancel order #" + o.getOrderId() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES && orderDAO.updateOrderStatus(o.getOrderId(), "Cancelled"))
                            loadOrders();
                    });
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                Order o = getTableRow().getItem();
                boolean canAdvance = nextStatus(o.getStatus()) != null;
                boolean canCancel  = !"Delivered".equals(o.getStatus()) && !"Cancelled".equals(o.getStatus());
                confirmBtn.setVisible(canAdvance);
                confirmBtn.setManaged(canAdvance);
                cancelBtn.setVisible(canCancel);
                cancelBtn.setManaged(canCancel);
                String next = nextStatus(o.getStatus());
                if (next != null) confirmBtn.setText("→ " + next);
                setGraphic(box);
            }
        });
    }

    private String nextStatus(String current) {
        return switch (current) {
            case "Processing" -> "Confirmed";
            case "Confirmed"  -> "Picking";
            case "Picking"    -> "Shipped";
            case "Shipped"    -> "Delivered";
            default           -> null;
        };
    }
}
