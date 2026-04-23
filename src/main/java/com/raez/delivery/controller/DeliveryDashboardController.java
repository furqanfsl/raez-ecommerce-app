package com.raez.delivery.controller;

import com.raez.delivery.dao.DeliveryDashboardDAO;
import com.raez.model.NavigationRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DeliveryDashboardController {

    @FXML private Label totalOrdersLabel;
    @FXML private Label inTransitLabel;
    @FXML private Label deliveredTodayLabel;
    @FXML private Label pendingLabel;
    @FXML private ListView<String> recentOrdersList;
    @FXML private ListView<String> alertsList;

    @FXML
    public void initialize() {
        totalOrdersLabel.setText(String.valueOf(DeliveryDashboardDAO.getTotalDeliveries()));
        inTransitLabel.setText(String.valueOf(DeliveryDashboardDAO.getStatusCount("in-transit")));
        deliveredTodayLabel.setText(String.valueOf(DeliveryDashboardDAO.getStatusCount("delivered")));
        pendingLabel.setText(String.valueOf(DeliveryDashboardDAO.getStatusCount("Pending")));

        recentOrdersList.getItems().clear();
        recentOrdersList.getItems().addAll(DeliveryDashboardDAO.getRecentDeliveries());

        alertsList.getItems().clear();
        alertsList.getItems().add("Total Drivers: " + DeliveryDashboardDAO.getTotalDrivers());
    }

    @FXML private void goToDrivers() { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesDrivers.fxml"); }
    @FXML private void goToOrders()  { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesOrders.fxml"); }
    @FXML private void logout()      { NavigationRouter.getInstance().logout(); }
}
