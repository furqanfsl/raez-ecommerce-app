package controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import database.DeliveryDAO;
import database.DriverDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import models.Delivery;
import utils.SceneSwitcher;

public class OrdersController {

    public static String selectedOrderId = "";

    @FXML
    private TextField searchField;

    @FXML
    private VBox addOrderFormBox;

    @FXML
    private TextField customerField;

    @FXML
    private TextField addressField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> driverComboBox;

    @FXML
    private TextField itemsField;

    @FXML
    private TableView<Delivery> ordersTable;

    @FXML
    private TableColumn<Delivery, String> idCol;

    @FXML
    private TableColumn<Delivery, String> customerCol;

    @FXML
    private TableColumn<Delivery, String> addressCol;

    @FXML
    private TableColumn<Delivery, String> statusCol;

    @FXML
    private TableColumn<Delivery, String> driverCol;

    @FXML
    private TableColumn<Delivery, String> itemsCol;

    @FXML
    private TableColumn<Delivery, String> dateCol;

    private final ObservableList<Delivery> deliveries = FXCollections.observableArrayList();
    private FilteredList<Delivery> filteredDeliveries;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> data.getValue().deliveryIdProperty());
        customerCol.setCellValueFactory(data -> data.getValue().orderIdProperty());
        addressCol.setCellValueFactory(data -> data.getValue().customerAddressProperty());
        statusCol.setCellValueFactory(data -> data.getValue().orderStatusProperty());
        driverCol.setCellValueFactory(data -> data.getValue().driverIdProperty());
        itemsCol.setCellValueFactory(data -> data.getValue().numOfItemsProperty());
        dateCol.setCellValueFactory(data -> data.getValue().orderDateProperty());

        statusComboBox.getItems().clear();
        statusComboBox.getItems().addAll("pending", "processing", "in-transit", "delivered", "cancelled");
        statusComboBox.setValue("pending");

        loadDriverIds();

        deliveries.setAll(DeliveryDAO.getAllDeliveries());

        filteredDeliveries = new FilteredList<>(deliveries, delivery -> true);
        ordersTable.setItems(filteredDeliveries);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.toLowerCase().trim();

            filteredDeliveries.setPredicate(delivery -> {
                if (query.isEmpty()) {
                    return true;
                }

                return delivery.getDeliveryId().toLowerCase().contains(query)
                        || delivery.getOrderId().toLowerCase().contains(query)
                        || delivery.getCustomerAddress().toLowerCase().contains(query)
                        || delivery.getOrderStatus().toLowerCase().contains(query)
                        || delivery.getDriverId().toLowerCase().contains(query);
            });
        });

        addOrderFormBox.setManaged(false);
        addOrderFormBox.setVisible(false);
    }

    private void loadDriverIds() {
        driverComboBox.getItems().clear();
        driverComboBox.getItems().addAll(DriverDAO.getAllDriverIds());

        if (!driverComboBox.getItems().isEmpty()) {
            driverComboBox.setValue(driverComboBox.getItems().get(0));
        }
    }

    @FXML
    private void toggleForm() {
        boolean show = !addOrderFormBox.isVisible();
        addOrderFormBox.setVisible(show);
        addOrderFormBox.setManaged(show);

        if (show) {
            loadDriverIds();
        }
    }

    @FXML
    private void addOrder() {
        String address = addressField.getText().trim();
        String status = statusComboBox.getValue();
        String driver = driverComboBox.getValue();
        String items = itemsField.getText().trim();

        if (address.isEmpty() || items.isEmpty() || driver == null || driver.isEmpty()) {
            return;
        }

        String deliveryId = DeliveryDAO.getNextDeliveryId();
        String orderId = customerField.getText().trim().isEmpty()
                ? DeliveryDAO.getNextOrderId()
                : customerField.getText().trim();

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Delivery delivery = new Delivery(
                deliveryId,
                orderId,
                address,
                status,
                date,
                items,
                driver
        );

        boolean inserted = DeliveryDAO.addDelivery(delivery);
        if (inserted) {
            deliveries.setAll(DeliveryDAO.getAllDeliveries());

            customerField.clear();
            addressField.clear();
            itemsField.clear();
            statusComboBox.setValue("pending");
            loadDriverIds();

            addOrderFormBox.setVisible(false);
            addOrderFormBox.setManaged(false);
        }
    }

    @FXML
    private void openSelectedOrder() {
        Delivery selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedOrderId = selected.getDeliveryId();
            SceneSwitcher.switchScene("orderDetail.fxml", ordersTable);
        }
    }

    @FXML
    private void goBack() {
        SceneSwitcher.switchScene("dashboard.fxml", ordersTable);
    }
}