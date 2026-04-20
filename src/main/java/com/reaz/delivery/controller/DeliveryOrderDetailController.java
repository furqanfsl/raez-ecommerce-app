package com.reaz.delivery.controller;

import com.reaz.model.NavigationRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DeliveryOrderDetailController {

    @FXML private Label orderIdLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerEmailLabel;
    @FXML private Label customerPhoneLabel;
    @FXML private Label customerAddressLabel;
    @FXML private Label driverNameLabel;
    @FXML private Label driverPhoneLabel;
    @FXML private Label driverVehicleLabel;
    @FXML private Label statusLabel;
    @FXML private Label priorityLabel;
    @FXML private Label orderDateLabel;
    @FXML private Label estimatedDeliveryLabel;
    @FXML private Label notesLabel;
    @FXML private ListView<String> itemsList;
    @FXML private ListView<String> timelineList;

    @FXML
    public void initialize() {
        orderIdLabel.setText(DeliveryOrdersController.selectedDeliveryId);

        customerNameLabel.setText("N/A");
        customerEmailLabel.setText("N/A");
        customerPhoneLabel.setText("N/A");
        customerAddressLabel.setText("N/A");

        driverNameLabel.setText("N/A");
        driverPhoneLabel.setText("N/A");
        driverVehicleLabel.setText("N/A");

        statusLabel.setText("N/A");
        priorityLabel.setText("Standard");
        orderDateLabel.setText("N/A");
        estimatedDeliveryLabel.setText("N/A");
        notesLabel.setText("N/A");

        itemsList.getItems().setAll("See Orders table for full details.");
        timelineList.getItems().setAll("Order: " + DeliveryOrdersController.selectedDeliveryId);
    }

    @FXML private void goBack() { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesOrders.fxml"); }
}
