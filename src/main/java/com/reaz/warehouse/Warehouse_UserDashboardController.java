package com.reaz.warehouse;

import com.reaz.warehouse.service.WarehouseService;
import com.reaz.warehouse.util.Warehouse_ValidationUtil;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDashboardController — loads products from the database.
 * Shows real-time stock status with colour coding.
 * Supports filtering by warehouse and category (product line).
 */
public class Warehouse_UserDashboardController {

    // NEW: store the logged-in user's ID
    private int currentUserID = -1;
    public void setCurrentUserID(int userID) { this.currentUserID = userID; }

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> warehouseCombo;

    @FXML private TableView<ProductRow>            productsTable;
    @FXML private TableColumn<ProductRow, String>  nameCol;
    @FXML private TableColumn<ProductRow, Integer> qtyCol;
    @FXML private TableColumn<ProductRow, String>  warehouseCol;
    @FXML private TableColumn<ProductRow, String>  statusCol;

    private final ObservableList<ProductRow> allProducts = FXCollections.observableArrayList();
    private FilteredList<ProductRow> filtered;

    private final WarehouseService warehouseService = new WarehouseService();
    private Runnable onLogout;

    public void setOnLogout(Runnable onLogout) { this.onLogout = onLogout; }

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        qtyCol.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        warehouseCol.setCellValueFactory(c -> c.getValue().warehouseProperty());
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());

        // Status badge styling
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    javafx.scene.control.Label badge = new javafx.scene.control.Label(item);
                    if ("Low Stock".equals(item)) {
                        badge.setStyle(
                            "-fx-background-color: #fee2e2;" +
                            "-fx-text-fill: #dc2626;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 3 10;" +
                            "-fx-background-radius: 12;");
                    } else {
                        badge.setStyle(
                            "-fx-background-color: #dcfce7;" +
                            "-fx-text-fill: #16a34a;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 3 10;" +
                            "-fx-background-radius: 12;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Quantity colour coding
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(String.valueOf(item));
                    ProductRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null && "Low Stock".equals(row.getStatus())) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Load from DB
        loadProducts();

        filtered = new FilteredList<>(allProducts, p -> true);
        productsTable.setItems(filtered);

        // Populate warehouse dropdown
        Set<String> warehouses = allProducts.stream()
                .map(ProductRow::getWarehouse).collect(Collectors.toSet());
        warehouseCombo.getItems().add("all");
        warehouseCombo.getItems().addAll(warehouses.stream().sorted().collect(Collectors.toList()));
        warehouseCombo.setValue("all");

        // Populate category dropdown from product line
        Set<String> categories = allProducts.stream()
                .map(ProductRow::getCategory).collect(Collectors.toSet());
        categoryCombo.getItems().add("all");
        categoryCombo.getItems().addAll(categories.stream().sorted().collect(Collectors.toList()));
        categoryCombo.setValue("all");

        // Filter listeners
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        warehouseCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
        categoryCombo.valueProperty().addListener((obs, o, n) -> applyFilters());

        applyFilters();
    }

    private void loadProducts() {
        ObservableList<WarehouseService.UserProductRow> dbRows =
                warehouseService.loadUserProductsFromDb();

        if (!dbRows.isEmpty()) {
            for (WarehouseService.UserProductRow row : dbRows) {
                allProducts.add(new ProductRow(
                        row.getName(),
                        row.getSku(),
                        row.getQuantity(),
                        row.getMinThreshold(),
                        row.getWarehouse(),
                        row.getCategory()
                ));
            }
        } else {
            seedProducts();
        }
    }

    private void applyFilters() {
        final String q   = Warehouse_ValidationUtil.safe(searchField.getText()).toLowerCase();
        final String wh  = Warehouse_ValidationUtil.safe(warehouseCombo.getValue());
        final String cat = Warehouse_ValidationUtil.safe(categoryCombo.getValue());

        filtered.setPredicate(p -> {
            boolean matchesSearch    = q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getSku().toLowerCase().contains(q);
            boolean matchesWarehouse = "all".equalsIgnoreCase(wh) || p.getWarehouse().equals(wh);
            boolean matchesCategory  = "all".equalsIgnoreCase(cat) || p.getCategory().equals(cat);
            return matchesSearch && matchesWarehouse && matchesCategory;
        });
    }

    @FXML private void onLogout() { if (onLogout != null) onLogout.run(); }

    // Fallback seed data
    private void seedProducts() {
        add("Robot Arm Type A",        "RBT-ARM-A01", 12,  10, "North Robotics Hub - Manchester, UK", "Robotics Parts");
        add("Robot Arm Type B",        "RBT-ARM-B02",  5,   8, "North Robotics Hub - Manchester, UK", "Robotics Parts");
        add("LIDAR Sensor Pack",       "SNS-LID-120",  3,   5, "North Robotics Hub - Manchester, UK", "Robotics Parts");
        add("Microcontroller Unit v2", "MCU-CTL-V20",  6,  10, "Midlands Automation Depot - Birmingham, UK", "Automation Equipment");
        add("Gyroscope Module",        "GYR-MOD-150",  7,  12, "South Robotics Storage - London, UK", "Mixed Robotics Items");
        add("Proximity Sensor IR",     "PRX-SNS-340",  8,  15, "East Engineering Warehouse - Norwich, UK", "Sensors and Cables");
    }

    private void add(String name, String sku, int qty, int min, String warehouse, String category) {
        allProducts.add(new ProductRow(name, sku, qty, min, warehouse, category));
    }

    // ── ProductRow model ──
    public static class ProductRow {
        private final StringProperty  name         = new SimpleStringProperty();
        private final StringProperty  sku          = new SimpleStringProperty();
        private final IntegerProperty quantity     = new SimpleIntegerProperty();
        private final IntegerProperty minThreshold = new SimpleIntegerProperty();
        private final StringProperty  warehouse    = new SimpleStringProperty();
        private final StringProperty  category     = new SimpleStringProperty();
        private final StringProperty  status       = new SimpleStringProperty();

        public ProductRow(String name, String sku, int quantity,
                          int minThreshold, String warehouse, String category) {
            this.name.set(name);
            this.sku.set(sku);
            this.quantity.set(quantity);
            this.minThreshold.set(minThreshold);
            this.warehouse.set(warehouse);
            this.category.set(category != null ? category : "General");
            this.status.set(quantity < minThreshold ? "Low Stock" : "In Stock");
        }

        public String getName()         { return name.get(); }
        public String getSku()          { return sku.get(); }
        public int    getQuantity()     { return quantity.get(); }
        public int    getMinThreshold() { return minThreshold.get(); }
        public String getWarehouse()    { return warehouse.get(); }
        public String getCategory()     { return category.get(); }
        public String getStatus()       { return status.get(); }

        public StringProperty  nameProperty()     { return name; }
        public StringProperty  skuProperty()      { return sku; }
        public IntegerProperty quantityProperty() { return quantity; }
        public StringProperty  warehouseProperty(){ return warehouse; }
        public StringProperty  categoryProperty() { return category; }
        public StringProperty  statusProperty()   { return status; }
    }
}