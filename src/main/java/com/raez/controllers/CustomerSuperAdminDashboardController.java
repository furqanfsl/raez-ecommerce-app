package com.raez.controllers;

import com.raez.customer.dao.CustomerAdminDAO;
import com.raez.customer.dao.CustomerOrderDAO;
import com.raez.customer.model.CustomerUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class CustomerSuperAdminDashboardController {

    @FXML private Label                             totalCustomersLabel;
    @FXML private Label                             totalRevenueLabel;
    @FXML private Label                             activeAccountsLabel;
    @FXML private Label                             deactivatedAccountsLabel;
    @FXML private TextField                         searchField;
    @FXML private ComboBox<String>                  statusFilter;
    @FXML private Label                             noResultsLabel;
    @FXML private TableView<CustomerUser>           customersTable;
    @FXML private TableColumn<CustomerUser, String> colName;
    @FXML private TableColumn<CustomerUser, String> colEmail;
    @FXML private TableColumn<CustomerUser, String> colStatus;
    @FXML private TableColumn<CustomerUser, String> colSpent;
    @FXML private TableColumn<CustomerUser, String> colAction;

    private final CustomerAdminDAO adminDAO = new CustomerAdminDAO();
    private final CustomerOrderDAO orderDAO = new CustomerOrderDAO();
    private CustomerUser currentUser;

    public void setUser(CustomerUser user) { this.currentUser = user; }

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "ACTIVE", "INACTIVE"));
        statusFilter.setValue("All");

        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colSpent.setCellValueFactory(d -> {
            try {
                double spent = orderDAO.getTotalSpentByUserId(d.getValue().getId());
                return new SimpleStringProperty("£" + String.format("%,.2f", spent));
            } catch (Exception e) { return new SimpleStringProperty("£0.00"); }
        });
        colAction.setCellValueFactory(d -> new SimpleStringProperty("Edit"));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Edit");
            { btn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-padding: 4 12;");
              btn.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        loadAnalytics();
        loadAllCustomers();
    }

    private void loadAnalytics() {
        try {
            int total  = adminDAO.getAllCustomers().size();
            double rev = adminDAO.getTotalRevenue();
            int act    = adminDAO.countByStatus("ACTIVE");
            int inact  = adminDAO.countByStatus("INACTIVE");
            totalCustomersLabel.setText(String.valueOf(total));
            totalRevenueLabel.setText("£" + String.format("%,.2f", rev));
            activeAccountsLabel.setText(String.valueOf(act));
            deactivatedAccountsLabel.setText(String.valueOf(inact));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadAllCustomers() {
        try {
            List<CustomerUser> list = adminDAO.getAllCustomers();
            customersTable.setItems(FXCollections.observableArrayList(list));
            setNoResults(list.isEmpty());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSearch() {
        try {
            String keyword = searchField.getText().trim();
            String status  = statusFilter.getValue() != null ? statusFilter.getValue() : "All";
            List<CustomerUser> list = adminDAO.searchCustomers(keyword, status);
            customersTable.setItems(FXCollections.observableArrayList(list));
            setNoResults(list.isEmpty());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleOpenPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerSuperAdminPanel.fxml"));
            Stage stage = (Stage) customersTable.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerSuperAdminPanelController ctrl = loader.getController();
            ctrl.setAdminUser(currentUser);
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerWelcome.fxml"));
            Stage stage = (Stage) customersTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), stage.getWidth(), stage.getHeight()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openEdit(CustomerUser customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CustomerSuperAdminEdit.fxml"));
            Stage stage = (Stage) customersTable.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerSuperAdminEditController ctrl = loader.getController();
            ctrl.setContext(currentUser, customer);
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setNoResults(boolean show) {
        noResultsLabel.setVisible(show);
        noResultsLabel.setManaged(show);
    }
}
