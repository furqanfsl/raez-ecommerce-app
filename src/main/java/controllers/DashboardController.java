package controllers;

import database.DashboardDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import utils.SceneSwitcher;

public class DashboardController {

    @FXML
    private Label totalOrdersLabel;

    @FXML
    private Label inTransitLabel;

    @FXML
    private Label deliveredTodayLabel;

    @FXML
    private Label pendingLabel;

    @FXML
    private ListView<String> recentOrdersList;

    @FXML
    private ListView<String> alertsList;

    @FXML
    public void initialize() {
        totalOrdersLabel.setText(String.valueOf(DashboardDAO.getTotalDeliveries()));
        inTransitLabel.setText(String.valueOf(DashboardDAO.getStatusCount("in-transit")));
        deliveredTodayLabel.setText(String.valueOf(DashboardDAO.getStatusCount("delivered")));
        pendingLabel.setText(String.valueOf(DashboardDAO.getStatusCount("Pending")));

        recentOrdersList.getItems().clear();
        recentOrdersList.getItems().addAll(DashboardDAO.getRecentDeliveries());

        alertsList.getItems().clear();
        alertsList.getItems().add("Total Drivers: " + DashboardDAO.getTotalDrivers());
    }

    @FXML
    private void goToDrivers() {
        SceneSwitcher.switchScene("DeliveriesDrivers.fxml", totalOrdersLabel);
    }

    @FXML
    private void goToOrders() {
        SceneSwitcher.switchScene("DeliveriesOrders.fxml", totalOrdersLabel);
    }

    @FXML
    private void logout() {
        SceneSwitcher.switchScene("DeliveriesLogin.fxml", totalOrdersLabel);
    }
}