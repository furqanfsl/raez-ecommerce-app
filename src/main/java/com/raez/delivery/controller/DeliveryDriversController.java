package com.raez.delivery.controller;

import com.raez.delivery.dao.DriverDAO;
import com.raez.delivery.model.DeliveryDriver;
import com.raez.model.NavigationRouter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class DeliveryDriversController {

    @FXML private TextField searchField;
    @FXML private VBox addFormBox;
    @FXML private TextField nameField;
    @FXML private TextField licenseField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    @FXML private TableView<DeliveryDriver> driversTable;
    @FXML private TableColumn<DeliveryDriver, String> idCol;
    @FXML private TableColumn<DeliveryDriver, String> nameCol;
    @FXML private TableColumn<DeliveryDriver, String> licenseCol;
    @FXML private TableColumn<DeliveryDriver, String> phoneCol;
    @FXML private TableColumn<DeliveryDriver, String> emailCol;

    private final ObservableList<DeliveryDriver> drivers = FXCollections.observableArrayList();
    private FilteredList<DeliveryDriver> filteredDrivers;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(d -> d.getValue().idProperty());
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        licenseCol.setCellValueFactory(d -> d.getValue().licenseNumberProperty());
        phoneCol.setCellValueFactory(d -> d.getValue().phoneProperty());
        emailCol.setCellValueFactory(d -> d.getValue().emailProperty());

        drivers.setAll(DriverDAO.getAllDrivers());
        filteredDrivers = new FilteredList<>(drivers, x -> true);
        driversTable.setItems(filteredDrivers);

        searchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.toLowerCase().trim();
            filteredDrivers.setPredicate(d -> q.isEmpty()
                    || d.getId().toLowerCase().contains(q)
                    || d.getName().toLowerCase().contains(q)
                    || d.getLicenseNumber().toLowerCase().contains(q)
                    || d.getPhone().toLowerCase().contains(q)
                    || d.getEmail().toLowerCase().contains(q));
        });

        addFormBox.setManaged(false);
        addFormBox.setVisible(false);
    }

    @FXML
    private void toggleForm() {
        boolean show = !addFormBox.isVisible();
        addFormBox.setVisible(show);
        addFormBox.setManaged(show);
    }

    @FXML
    private void addDriver() {
        String name    = nameField.getText().trim();
        String license = licenseField.getText().trim();
        String phone   = phoneField.getText().trim();
        String email   = emailField.getText().trim();
        if (name.isEmpty() || license.isEmpty() || phone.isEmpty() || email.isEmpty()) return;

        if (DriverDAO.addDriver(new DeliveryDriver("", name, license, phone, email))) {
            drivers.setAll(DriverDAO.getAllDrivers());
            nameField.clear(); licenseField.clear(); phoneField.clear(); emailField.clear();
            addFormBox.setVisible(false); addFormBox.setManaged(false);
        }
    }

    @FXML
    private void deleteSelectedDriver() {
        DeliveryDriver selected = driversTable.getSelectionModel().getSelectedItem();
        if (selected != null && DriverDAO.deleteDriver(selected.getId()))
            drivers.remove(selected);
    }

    @FXML private void goBack() { NavigationRouter.getInstance().navigateTo("/fxml/DeliveriesDashboard.fxml"); }
}
