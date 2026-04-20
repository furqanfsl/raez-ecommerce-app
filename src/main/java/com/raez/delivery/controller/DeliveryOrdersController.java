package com.raez.delivery.controller;

import com.raez.delivery.dao.DeliveryDAO;
import com.raez.delivery.dao.DriverDAO;
import com.raez.delivery.model.DeliveryDelivery;
import com.raez.model.NavigationRouter;
import com.raez.orders.dao.OrderDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeliveryOrdersController {

    public static String selectedDeliveryId = "";

    @FXML private TextField searchField;
    @FXML private VBox addOrderFormBox;
    @FXML private TextField customerField;
    @FXML private TextField addressField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ComboBox<String> driverComboBox;
    @FXML private TextField itemsField;

    @FXML private TableView<DeliveryDelivery> ordersTable;
    @FXML private TableColumn<DeliveryDelivery, String> idCol;
    @FXML private TableColumn<DeliveryDelivery, String> customerCol;
    @FXML private TableColumn<DeliveryDelivery, String> addressCol;
    @FXML private TableColumn<DeliveryDelivery, String> statusCol;
    @FXML private TableColumn<DeliveryDelivery, String> driverCol;
    @FXML private TableColumn<DeliveryDelivery, String> itemsCol;
    @FXML private TableColumn<DeliveryDelivery, String> dateCol;
    @FXML private TableColumn<DeliveryDelivery, Void>   deliverCol;

    private final ObservableList<DeliveryDelivery> deliveries = FXCollections.observableArrayList();
    private final OrderDAO orderDAO = new OrderDAO();
    private FilteredList<DeliveryDelivery> filteredDeliveries;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(d -> d.getValue().deliveryIdProperty());
        customerCol.setCellValueFactory(d -> d.getValue().orderIdProperty());
        addressCol.setCellValueFactory(d -> d.getValue().customerAddressProperty());
        statusCol.setCellValueFactory(d -> d.getValue().orderStatusProperty());
        driverCol.setCellValueFactory(d -> d.getValue().driverIdProperty());
        itemsCol.setCellValueFactory(d -> d.getValue().numOfItemsProperty());
        dateCol.setCellValueFactory(d -> d.getValue().orderDateProperty());

        statusComboBox.getItems().setAll("Pending", "processing", "in-transit", "delivered", "cancelled");
        statusComboBox.setValue("Pending");

        loadDriverIds();

        deliveries.setAll(DeliveryDAO.getAllDeliveries());
        filteredDeliveries = new FilteredList<>(deliveries, x -> true);
        ordersTable.setItems(filteredDeliveries);

        searchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.toLowerCase().trim();
            filteredDeliveries.setPredicate(d -> q.isEmpty()
                    || d.getDeliveryId().toLowerCase().contains(q)
                    || d.getOrderId().toLowerCase().contains(q)
                    || d.getCustomerAddress().toLowerCase().contains(q)
                    || d.getOrderStatus().toLowerCase().contains(q)
                    || d.getDriverId().toLowerCase().contains(q));
        });

        addOrderFormBox.setManaged(false);
        addOrderFormBox.setVisible(false);

        wireDeliverColumn();
    }

    private void wireDeliverColumn() {
        if (deliverCol == null) return;
        deliverCol.setCellFactory(col -> new TableCell<>() {
            private final Button deliverBtn = new Button("✔ Deliver");
            {
                deliverBtn.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:5;-fx-cursor:hand;");
                deliverBtn.setOnAction(e -> {
                    DeliveryDelivery row = getTableRow().getItem();
                    if (row == null) return;
                    int deliveryId = Integer.parseInt(row.getDeliveryId());
                    if (orderDAO.markDelivered(deliveryId)) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION,
                                "Delivery #" + deliveryId + " marked as Delivered.\nOrder status updated to Delivered.");
                        alert.setTitle("Delivered ✅");
                        alert.showAndWait();
                        deliveries.setAll(DeliveryDAO.getAllDeliveries());
                    } else {
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                                "Could not update delivery status.").showAndWait();
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                DeliveryDelivery row = getTableRow().getItem();
                boolean pending = row != null && ("Pending".equals(row.getOrderStatus()) ||
                        "Assigned".equals(row.getOrderStatus()) || "In Transit".equals(row.getOrderStatus()));
                setGraphic(pending ? deliverBtn : null);
            }
        });
    }

    private void loadDriverIds() {
        driverComboBox.getItems().setAll(DriverDAO.getAllDriverIds());
        if (!driverComboBox.getItems().isEmpty())
            driverComboBox.setValue(driverComboBox.getItems().get(0));
    }

    @FXML
    private void toggleForm() {
        boolean show = !addOrderFormBox.isVisible();
        addOrderFormBox.setVisible(show);
        addOrderFormBox.setManaged(show);
        if (show) loadDriverIds();
    }

    @FXML
    private void addOrder() {
        String address = addressField.getText().trim();
        String status  = statusComboBox.getValue();
        String driver  = driverComboBox.getValue();
        String items   = itemsField.getText().trim();
        if (address.isEmpty() || items.isEmpty() || driver == null || driver.isEmpty()) return;

        String orderId = customerField.getText().trim().isEmpty()
                ? String.valueOf(System.currentTimeMillis()) : customerField.getText().trim();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        DeliveryDelivery delivery = new DeliveryDelivery("0", orderId, address, status, date, items, driver);
        if (DeliveryDAO.addDelivery(delivery)) {
            deliveries.setAll(DeliveryDAO.getAllDeliveries());
            customerField.clear(); addressField.clear(); itemsField.clear();
            statusComboBox.setValue("Pending"); loadDriverIds();
            addOrderFormBox.setVisible(false); addOrderFormBox.setManaged(false);
        }
    }

    @FXML
    private void openSelectedOrder() {
        DeliveryDelivery selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedDeliveryId = selected.getDeliveryId();
            NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesOrderDetail.fxml");
        }
    }

    @FXML private void goBack() { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesDashboard.fxml"); }
}
