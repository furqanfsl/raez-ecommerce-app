package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import utils.SceneSwitcher;

public class OrderDetailController {

    @FXML
    private Label orderIdLabel;

    @FXML
    private Label customerNameLabel;

    @FXML
    private Label customerEmailLabel;

    @FXML
    private Label customerPhoneLabel;

    @FXML
    private Label customerAddressLabel;

    @FXML
    private Label driverNameLabel;

    @FXML
    private Label driverPhoneLabel;

    @FXML
    private Label driverVehicleLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label priorityLabel;

    @FXML
    private Label orderDateLabel;

    @FXML
    private Label estimatedDeliveryLabel;

    @FXML
    private Label notesLabel;

    @FXML
    private ListView<String> itemsList;

    @FXML
    private ListView<String> timelineList;

    @FXML
    public void initialize() {
        String orderId = OrdersController.selectedOrderId;
        orderIdLabel.setText(orderId);

        customerNameLabel.setText("John Smith");
        customerEmailLabel.setText("john.smith@email.com");
        customerPhoneLabel.setText("+1 (555) 123-4567");
        customerAddressLabel.setText("123 Main St, New York, NY 10001");

        driverNameLabel.setText("Mike Johnson");
        driverPhoneLabel.setText("+1 (555) 987-6543");
        driverVehicleLabel.setText("Toyota Camry - ABC 123");

        statusLabel.setText("In Transit");
        priorityLabel.setText("Express");
        orderDateLabel.setText("2025-11-13 09:30");
        estimatedDeliveryLabel.setText("2025-11-13 15:00");
        notesLabel.setText("Please call upon arrival. Gate code: 1234");

        itemsList.getItems().addAll(
                "Electronics Package - Quantity: 1 - Weight: 2.5 kg",
                "Office Supplies - Quantity: 3 - Weight: 1.2 kg",
                "Documents Envelope - Quantity: 1 - Weight: 0.3 kg"
        );

        timelineList.getItems().addAll(
                "Order Placed - 2025-11-13 09:30",
                "Order Confirmed - 2025-11-13 09:45",
                "Processing - 2025-11-13 10:00",
                "Out for Delivery - 2025-11-13 11:30",
                "Delivered - Estimated 15:00"
        );
    }

    @FXML
    private void goBack() {
        SceneSwitcher.switchScene("orders.fxml", orderIdLabel);
    }
}