package controllers;

import database.DriverDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import models.Driver;
import utils.SceneSwitcher;

public class DriversController {

    @FXML
    private TextField searchField;

    @FXML
    private VBox addFormBox;

    @FXML
    private TextField nameField;

    @FXML
    private TextField licenseField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TableView<Driver> driversTable;

    @FXML
    private TableColumn<Driver, String> idCol;

    @FXML
    private TableColumn<Driver, String> nameCol;

    @FXML
    private TableColumn<Driver, String> licenseCol;

    @FXML
    private TableColumn<Driver, String> phoneCol;

    @FXML
    private TableColumn<Driver, String> emailCol;

    private final ObservableList<Driver> drivers = FXCollections.observableArrayList();
    private FilteredList<Driver> filteredDrivers;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        licenseCol.setCellValueFactory(data -> data.getValue().licenseNumberProperty());
        phoneCol.setCellValueFactory(data -> data.getValue().phoneProperty());
        emailCol.setCellValueFactory(data -> data.getValue().emailProperty());

        drivers.setAll(DriverDAO.getAllDrivers());

        filteredDrivers = new FilteredList<>(drivers, driver -> true);
        driversTable.setItems(filteredDrivers);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.toLowerCase().trim();

            filteredDrivers.setPredicate(driver -> {
                if (query.isEmpty()) {
                    return true;
                }

                return driver.getId().toLowerCase().contains(query)
                        || driver.getName().toLowerCase().contains(query)
                        || driver.getLicenseNumber().toLowerCase().contains(query)
                        || driver.getPhone().toLowerCase().contains(query)
                        || driver.getEmail().toLowerCase().contains(query);
            });
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
        String name = nameField.getText().trim();
        String license = licenseField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || license.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            return;
        }

        Driver driver = new Driver("", name, license, phone, email);

        boolean inserted = DriverDAO.addDriver(driver);
        if (inserted) {
            drivers.setAll(DriverDAO.getAllDrivers());

            nameField.clear();
            licenseField.clear();
            phoneField.clear();
            emailField.clear();

            addFormBox.setVisible(false);
            addFormBox.setManaged(false);
        }
    }

    @FXML
    private void deleteSelectedDriver() {
        Driver selected = driversTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean deleted = DriverDAO.deleteDriver(selected.getId());
            if (deleted) {
                drivers.remove(selected);
            }
        }
    }

    @FXML
    private void goBack() {
        SceneSwitcher.switchScene("dashboard.fxml", driversTable);
    }
}