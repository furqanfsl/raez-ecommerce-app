package com.raez.controllers;

import com.raez.customer.dao.CustomerAdminDAO;
import com.raez.customer.dao.CustomerOrderDAO;
import com.raez.customer.model.CustomerUser;
import com.raez.model.NavigationRouter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
// FXMLLoader/Scene/Stage retained for openEdit() navigation

import java.util.List;

public class CustomerAdminDashboardController {

    @FXML private Label                        totalCustomersLabel;
    @FXML private Label                        totalRevenueLabel;
    @FXML private Label                        activeAccountsLabel;
    @FXML private Label                        deactivatedAccountsLabel;
    @FXML private TextField                    searchField;
    @FXML private ComboBox<String>             statusFilter;
    @FXML private Label                        noResultsLabel;
    @FXML private TableView<CustomerUser>      customersTable;
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
            { btn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-padding: 4 12;");
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
            int total    = adminDAO.getAllCustomers().size();
            double rev   = adminDAO.getTotalRevenue();
            int active   = adminDAO.countByStatus("ACTIVE");
            int inactive = adminDAO.countByStatus("INACTIVE");
            totalCustomersLabel.setText(String.valueOf(total));
            totalRevenueLabel.setText("£" + String.format("%,.2f", rev));
            activeAccountsLabel.setText(String.valueOf(active));
            deactivatedAccountsLabel.setText(String.valueOf(inactive));
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
    private void handleGoToStorefront() {
        NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml");
    }

    @FXML
    private void handleLogout() {
        NavigationRouter.getInstance().logout();
    }

    private void openEdit(CustomerUser customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerStaffEdit.fxml"));
            Stage stage = (Stage) customersTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), stage.getWidth(), stage.getHeight()));
            CustomerStaffEditController ctrl = loader.getController();
            ctrl.setContext(currentUser, customer);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setNoResults(boolean show) {
        noResultsLabel.setVisible(show);
        noResultsLabel.setManaged(show);
    }
}
