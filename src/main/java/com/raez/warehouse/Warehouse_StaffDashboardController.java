package com.raez.warehouse;

import com.raez.model.NavigationRouter;
import com.raez.orders.dao.OrderDAO;
import com.raez.orders.model.Order;
import com.raez.warehouse.model.Warehouse_LowStockRow;
import com.raez.warehouse.model.Warehouse_Product;
import com.raez.warehouse.model.Warehouse;
import com.raez.warehouse.service.Warehouse_DeliveryService;
import com.raez.warehouse.service.Warehouse_ReportService;
import com.raez.warehouse.service.WarehouseService;
import com.raez.warehouse.util.Warehouse_DialogUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Set;
import java.util.stream.Collectors;

public class Warehouse_StaffDashboardController {

    private final WarehouseService           warehouseService           = new WarehouseService();
    private final Warehouse_DeliveryService  warehouse_DeliveryService  = new Warehouse_DeliveryService();
    private final OrderDAO                   orderDAO                   = new OrderDAO();

    // NEW: store the logged-in user's ID for audit logging
    private int currentUserID = -1;
    public void setCurrentUserID(int userID) { this.currentUserID = userID; }

    @FXML private Button toggleWarehousesBtn;
    @FXML private Button toggleReportBtn;

    @FXML private VBox lowStockCard;
    @FXML private TableView<Warehouse_LowStockRow> lowStockTable;
    @FXML private TableColumn<Warehouse_LowStockRow, String>  lsProductCol;
    @FXML private TableColumn<Warehouse_LowStockRow, String>  lsSkuCol;
    @FXML private TableColumn<Warehouse_LowStockRow, Integer> lsQtyCol;
    @FXML private TableColumn<Warehouse_LowStockRow, String>  lsWarehouseCol;

    @FXML private VBox  reportCard;
    @FXML private Label totalStockLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label capacityUsageLabel;

    @FXML private VBox tablesSplit;

    @FXML private TableView<Warehouse> warehousesTable;
    @FXML private TableColumn<Warehouse, String>  wIdCol;
    @FXML private TableColumn<Warehouse, String>  wNameCol;
    @FXML private TableColumn<Warehouse, String>  wLocationCol;
    @FXML private TableColumn<Warehouse, String>  wLineCol;
    @FXML private TableColumn<Warehouse, String>  wCapacityCol;
    @FXML private TableColumn<Warehouse, Integer> wLowCountCol;
    @FXML private TableColumn<Warehouse, Void>    wActionsCol;

    @FXML private Label              productsTitle;
    @FXML private TableView<Warehouse_Product> productsTable;
    @FXML private TableColumn<Warehouse_Product, String>  pNameCol;
    @FXML private TableColumn<Warehouse_Product, String>  pSkuCol;
    @FXML private TableColumn<Warehouse_Product, Integer> pQtyCol;
    @FXML private TableColumn<Warehouse_Product, Integer> pMinCol;
    @FXML private TableColumn<Warehouse_Product, String>  pRestockCol;
    @FXML private TableColumn<Warehouse_Product, Integer> pReorderCol;

    @FXML private VBox pendingOrdersCard;
    @FXML private TableView<Order>     pendingOrdersTable;
    @FXML private TableColumn<Order, Integer> poIdCol;
    @FXML private TableColumn<Order, String>  poCustomerCol;
    @FXML private TableColumn<Order, String>  poDateCol;
    @FXML private TableColumn<Order, Double>  poTotalCol;
    @FXML private TableColumn<Order, Integer> poItemsCol;
    @FXML private TableColumn<Order, Void>    poPackCol;

    private final ObservableList<Order> pendingOrders = FXCollections.observableArrayList();

    @FXML private VBox deliveryCard;
    @FXML private TableView<Warehouse_DeliveryService.DeliveryRow>   deliveryTable;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delIdCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delOrderCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delAddressCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delItemsCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delDriverCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delWarehouseCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, String> delDateCol;
    @FXML private TableColumn<Warehouse_DeliveryService.DeliveryRow, Void>   delActionsCol;

    private final ObservableList<Warehouse>   warehouses      = FXCollections.observableArrayList();
    private final ObservableList<Warehouse_Product>     currentProducts = FXCollections.observableArrayList();
    private final ObservableList<Warehouse_LowStockRow> warehouse_LowStockRows    = FXCollections.observableArrayList();
    private final ObservableList<Warehouse_DeliveryService.DeliveryRow> deliveries = FXCollections.observableArrayList();

    private Runnable onLogout;
    public void setOnLogout(Runnable onLogout) { this.onLogout = onLogout; }

    @FXML private void onAddDemoDeliveries() {
        boolean ok = warehouse_DeliveryService.insertDemoDeliveries();
        if (ok) {
            refreshDeliveries();
            Warehouse_DialogUtil.info("Demo Deliveries Added",
                    "3 new pending deliveries have been added!\nYour teacher can now confirm or reject them.");
        } else {
            Warehouse_DialogUtil.info("Error", "Could not add demo deliveries.");
        }
    }

    @FXML
    private void initialize() {
        wireColumns();
        wireActionButtons();
        wireLowStockHighlight();
        wireDeliveryButtons();

        warehousesTable.setItems(warehouses);
        productsTable.setItems(currentProducts);
        lowStockTable.setItems(warehouse_LowStockRows);
        deliveryTable.setItems(deliveries);
        pendingOrdersTable.setItems(pendingOrders);

        wirePendingOrdersColumns();

        warehouses.setAll(warehouseService.loadWarehousesFromDb());

        warehousesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldW, newW) -> {
                    if (newW == null) {
                        currentProducts.clear();
                        productsTitle.setText("Products");
                    } else {
                        currentProducts.setAll(newW.getProducts());
                        productsTitle.setText("Products in " + newW.getName());
                    }
                });

        refreshComputedUI();
        refreshDeliveries();
        refreshPendingOrders();
        if (!warehouses.isEmpty()) warehousesTable.getSelectionModel().select(0);
    }

    private void wireColumns() {
        lsProductCol.setCellValueFactory(c -> c.getValue().productNameProperty());
        lsSkuCol.setCellValueFactory(c -> c.getValue().skuProperty());
        lsQtyCol.setCellValueFactory(c -> c.getValue().qtyProperty().asObject());
        lsWarehouseCol.setCellValueFactory(c -> c.getValue().warehouseLocationProperty());

        wIdCol.setCellValueFactory(c -> c.getValue().warehouseIdProperty());
        wNameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        wLocationCol.setCellValueFactory(c -> c.getValue().locationProperty());
        wLineCol.setCellValueFactory(c -> c.getValue().productLineProperty());
        wCapacityCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedCapacity()));
        wLowCountCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getLowStockCount()).asObject());

        pNameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        pSkuCol.setCellValueFactory(c -> c.getValue().skuProperty());
        pQtyCol.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        pMinCol.setCellValueFactory(c -> c.getValue().minThresholdProperty().asObject());
        pRestockCol.setCellValueFactory(c -> c.getValue().lastRestockProperty());
        pReorderCol.setCellValueFactory(c -> c.getValue().reorderLevelProperty().asObject());

        delIdCol.setCellValueFactory(c -> c.getValue().deliveryIdProperty());
        delOrderCol.setCellValueFactory(c -> c.getValue().orderIdProperty());
        delAddressCol.setCellValueFactory(c -> c.getValue().addressProperty());
        delItemsCol.setCellValueFactory(c -> c.getValue().numItemsProperty());
        delDriverCol.setCellValueFactory(c -> c.getValue().driverProperty());
        delWarehouseCol.setCellValueFactory(c -> c.getValue().warehouseNameProperty());
        delDateCol.setCellValueFactory(c -> c.getValue().orderDateProperty());
    }

    private void wireDeliveryButtons() {
        delActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button confirmBtn = new Button("✔ Confirm");
            private final Button rejectBtn  = new Button("✘ Reject");
            private final HBox   box        = new HBox(6, confirmBtn, rejectBtn);

            {
                confirmBtn.setStyle(
                    "-fx-background-color: #16a34a; -fx-text-fill: white;" +
                    "-fx-font-size:11px; -fx-padding: 4 10; -fx-background-radius: 5;");
                rejectBtn.setStyle(
                    "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                    "-fx-font-size:11px; -fx-padding: 4 10; -fx-background-radius: 5;");

                confirmBtn.setOnAction(e -> {
                    Warehouse_DeliveryService.DeliveryRow row = getTableRow().getItem();
                    if (row != null) {
                        Warehouse_DeliveryService.ConfirmResult result = warehouse_DeliveryService.confirmDelivery(
                                row.getDeliveryIdInt(),
                                row.getNumItemsInt(),
                                row.getWarehouseIdInt()
                        );
                        if (result.isSuccess()) {
                            warehouses.setAll(warehouseService.loadWarehousesFromDb());
                            refreshComputedUI();
                            refreshDeliveries();
                            Warehouse_DialogUtil.info("Delivery Confirmed! ✅",
                                    "Delivery #" + row.getDeliveryIdInt() + " to " +
                                    row.warehouseNameProperty().get() + " confirmed!\n" +
                                    row.getNumItemsInt() + " units have been added to warehouse stock.");
                        } else {
                            Warehouse_DialogUtil.info("Error", "Could not confirm delivery. Check database.");
                        }
                    }
                });

                rejectBtn.setOnAction(e -> {
                    Warehouse_DeliveryService.DeliveryRow row = getTableRow().getItem();
                    if (row != null && Warehouse_DialogUtil.confirm("Reject Delivery",
                            "Are you sure you want to reject delivery #" +
                            row.getDeliveryIdInt() + " to " +
                            row.warehouseNameProperty().get() + "?")) {
                        boolean ok = warehouse_DeliveryService.rejectDelivery(row.getDeliveryIdInt());
                        if (ok) {
                            refreshDeliveries();
                            Warehouse_DialogUtil.info("Rejected",
                                    "Delivery #" + row.getDeliveryIdInt() + " has been rejected.");
                        } else {
                            Warehouse_DialogUtil.info("Error", "Could not reject delivery.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void wireActionButtons() {
        wActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn   = new Button("View Stock");
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(4, viewBtn, editBtn, deleteBtn);

            {
                viewBtn.getStyleClass().add("primary-button");
                editBtn.getStyleClass().add("neutral-button");
                deleteBtn.getStyleClass().add("danger-button");
                viewBtn.setStyle("-fx-font-size:11px; -fx-padding: 4 8;");
                editBtn.setStyle("-fx-font-size:11px; -fx-padding: 4 8;");
                deleteBtn.setStyle("-fx-font-size:11px; -fx-padding: 4 8;");

                viewBtn.setOnAction(e -> {
                    Warehouse w = getTableRow().getItem();
                    if (w != null) {
                        warehousesTable.getSelectionModel().select(w);
                        currentProducts.setAll(w.getProducts());
                        productsTitle.setText("Products in " + w.getName());
                    }
                });
                editBtn.setOnAction(e -> {
                    warehousesTable.getSelectionModel().select(getTableRow().getItem());
                    onEditWarehouse();
                });
                deleteBtn.setOnAction(e -> {
                    warehousesTable.getSelectionModel().select(getTableRow().getItem());
                    onDeleteWarehouse();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void wireLowStockHighlight() {
        pQtyCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("low-stock-cell");
                } else {
                    setText(String.valueOf(item));
                    Warehouse_Product p = getTableRow() == null ? null : (Warehouse_Product) getTableRow().getItem();
                    boolean low = p != null && p.isLowStock();
                    if (low) {
                        if (!getStyleClass().contains("low-stock-cell")) getStyleClass().add("low-stock-cell");
                    } else {
                        getStyleClass().removeAll("low-stock-cell");
                    }
                }
            }
        });
    }

    @FXML private void onGoToStorefront() {
        NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml");
    }

    @FXML private void onLogout() { if (onLogout != null) onLogout.run(); }

    @FXML private void onToggleWarehouses() {
        boolean visible = tablesSplit.isVisible();
        tablesSplit.setVisible(!visible);
        tablesSplit.setManaged(!visible);
        toggleWarehousesBtn.setText(visible ? "Show Warehouses" : "Hide Warehouses");
    }

    @FXML private void onToggleReport() {
        boolean show = !reportCard.isVisible();
        reportCard.setVisible(show);
        reportCard.setManaged(show);
        toggleReportBtn.setText(show ? "Hide Report" : "Generate Report");
        refreshComputedUI();
    }

    @FXML private void onRefreshDeliveries() {
        refreshDeliveries();
        Warehouse_DialogUtil.info("Refreshed", "Delivery list has been refreshed.");
    }

    @FXML
    private void onAddWarehouse() {
        try {
            // CHANGED: AddWarehouseDialog.fxml → WarehouseAddDialog.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WarehouseAddDialog.fxml"));
            Stage dialogStage = buildDialog("Add Warehouse", loader, 520, 360);
            AddWarehouseDialogController c = loader.getController();
            c.setStage(dialogStage);
            Set<String> ids = warehouses.stream().map(Warehouse::getWarehouseId).collect(Collectors.toSet());
            c.setExistingWarehouseIds(ids);
            dialogStage.showAndWait();

            AddWarehouseDialogController.Result r = c.getResult();
            if (r != null) {
                int dbId = warehouseService.saveWarehouseToDb(r.name, r.location, "", r.maxCapacity);
                if (dbId == -1) { Warehouse_DialogUtil.info("Error", "Could not save warehouse to database."); return; }

                warehouses.setAll(warehouseService.loadWarehousesFromDb());
                refreshComputedUI();

                warehouses.stream()
                        .filter(w -> w.getId().equals(String.valueOf(dbId)))
                        .findFirst()
                        .ifPresent(w -> warehousesTable.getSelectionModel().select(w));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not open Add Warehouse dialog.");
        }
    }

    @FXML
    private void onAddProduct() {
        try {
            // CHANGED: AddProductDialog.fxml → WarehouseAddProductDialog.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WarehouseAddProductDialog.fxml"));
            Stage dialogStage = buildDialog("Add Product", loader, 540, 420);
            Warehouse_AddProductDialogController c = loader.getController();
            c.setStage(dialogStage);
            c.setWarehouses(warehouses, warehousesTable.getSelectionModel().getSelectedItem());
            dialogStage.showAndWait();

            Warehouse_AddProductDialogController.Result r = c.getResult();
            if (r != null) {
                WarehouseService.AddProductResult result = warehouseService.addProduct(
                        r.warehouse, r.name, r.sku, r.quantity,
                        r.minThreshold, r.reorderLevel, r.lastRestock);
                if (!result.isSuccess()) { Warehouse_DialogUtil.info("Error", result.getMessage()); return; }

                int warehouseDbId = Integer.parseInt(r.warehouse.getId());
                // CHANGED: now passes currentUserID for audit logging
                int productDbId   = warehouseService.saveProductToDb(
                        warehouseDbId, r.name, r.quantity,
                        r.minThreshold, r.reorderLevel, r.lastRestock, 0.0, currentUserID);
                if (productDbId == -1)
                    Warehouse_DialogUtil.info("Warning", "Product added in app but could not save to database.");

                Warehouse nowSelected = warehousesTable.getSelectionModel().getSelectedItem();
                if (nowSelected != null && nowSelected == r.warehouse)
                    currentProducts.setAll(nowSelected.getProducts());
                refreshComputedUI();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not open Add Product dialog.");
        }
    }

    @FXML
    private void onTransferStock() {
        try {
            // CHANGED: TransferStockDialog.fxml → WarehouseTransferStockDialog.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WarehouseTransferStockDialog.fxml"));
            Stage dialogStage = buildDialog("Transfer Stock", loader, 560, 340);
            Warehouse_TransferStockDialogController c = loader.getController();
            c.setStage(dialogStage);
            c.setWarehouses(warehouses);
            dialogStage.showAndWait();

            Warehouse_TransferStockDialogController.Result r = c.getResult();
            if (r != null) {
                WarehouseService.TransferResult result = warehouseService.transferStock(
                        r.from, r.to, r.warehouse_Product, r.quantity);
                if (!result.isSuccess()) { Warehouse_DialogUtil.info("Error", result.getMessage()); return; }

                int fromDbId    = Integer.parseInt(r.from.getId());
                int toDbId      = Integer.parseInt(r.to.getId());
                int productDbId = Integer.parseInt(r.warehouse_Product.getId());
                // CHANGED: now passes currentUserID for audit logging
                warehouseService.updateStockTransferInDb(productDbId, fromDbId, productDbId, toDbId, r.quantity, currentUserID);

                String selectedId = warehousesTable.getSelectionModel().getSelectedItem() != null
                        ? warehousesTable.getSelectionModel().getSelectedItem().getId() : null;

                warehouses.setAll(warehouseService.loadWarehousesFromDb());
                refreshComputedUI();

                if (selectedId != null) {
                    warehouses.stream()
                            .filter(w -> w.getId().equals(selectedId))
                            .findFirst()
                            .ifPresent(w -> {
                                warehousesTable.getSelectionModel().select(w);
                                currentProducts.setAll(w.getProducts());
                                productsTitle.setText("Products in " + w.getName());
                            });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not open Transfer Stock dialog.");
        }
    }

    @FXML
    private void onEditProduct() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        Warehouse_Product   p = productsTable.getSelectionModel().getSelectedItem();
        if (w == null || p == null) { Warehouse_DialogUtil.info("No selection", "Select a warehouse and a product first."); return; }

        try {
            // CHANGED: EditProductDialog.fxml → WarehouseEditProductDialog.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WarehouseEditProductDialog.fxml"));
            Stage dialogStage = buildDialog("Edit Product", loader, 520, 360);
            Warehouse_EditProductDialogController c = loader.getController();
            c.setStage(dialogStage);
            c.setData(w, p);
            dialogStage.showAndWait();

            Warehouse_EditProductDialogController.Result r = c.getResult();
            if (r != null) {
                WarehouseService.UpdateProductResult result = warehouseService.updateProduct(
                        w, p, r.name, r.sku, r.quantity, r.minThreshold, r.reorderLevel);
                if (!result.isSuccess()) { Warehouse_DialogUtil.info("Error", result.getMessage()); return; }

                try {
                    // CHANGED: now passes currentUserID for audit logging
                    warehouseService.updateProductInDb(Integer.parseInt(p.getId()),
                            Integer.parseInt(w.getId()), r.name, r.quantity,
                            r.minThreshold, r.reorderLevel, p.getLastRestock(), currentUserID);
                } catch (NumberFormatException ignored) {}

                for (int i = 0; i < w.getProducts().size(); i++) {
                    if (w.getProducts().get(i).getId().equals(p.getId())) {
                        w.getProducts().set(i, result.getProduct()); break;
                    }
                }
                currentProducts.setAll(w.getProducts());
                refreshComputedUI();
                productsTable.getSelectionModel().select(result.getProduct());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not open Edit Product dialog.");
        }
    }

    @FXML
    private void onEditWarehouse() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        if (w == null) { Warehouse_DialogUtil.info("No selection", "Select a warehouse first."); return; }

        try {
            // CHANGED: EditWarehouseDialog.fxml → WarehouseEditDialog.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WarehouseEditDialog.fxml"));
            Stage dialogStage = buildDialog("Edit Warehouse", loader, 520, 360);
            EditWarehouseDialogController c = loader.getController();
            c.setStage(dialogStage);
            Set<String> ids = warehouses.stream().map(Warehouse::getWarehouseId).collect(Collectors.toSet());
            c.setExistingWarehouseIds(ids);
            c.setWarehouse(w);
            dialogStage.showAndWait();

            Warehouse updated = c.getResult();
            if (updated != null) {
                try {
                    warehouseService.updateWarehouseInDb(Integer.parseInt(w.getId()),
                            updated.getName(), updated.getLocation(), "", updated.getMaxCapacity());
                } catch (NumberFormatException ignored) {}

                for (int i = 0; i < warehouses.size(); i++) {
                    if (warehouses.get(i).getId().equals(w.getId())) {
                        warehouses.set(i, updated); break;
                    }
                }
                refreshComputedUI();
                warehousesTable.getSelectionModel().select(updated);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not open Edit Warehouse dialog.");
        }
    }

    @FXML
    private void onDeleteWarehouse() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        if (w == null) { Warehouse_DialogUtil.info("No selection", "Select a warehouse first."); return; }

        if (Warehouse_DialogUtil.confirm("Delete warehouse?", "Are you sure you want to delete this warehouse?")) {
            try { warehouseService.deleteWarehouseFromDb(Integer.parseInt(w.getId())); }
            catch (NumberFormatException ignored) {}

            warehouses.setAll(warehouseService.loadWarehousesFromDb());
            currentProducts.clear();
            productsTitle.setText("Products");
            refreshComputedUI();
        }
    }

    @FXML
    private void onDeleteProduct() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        Warehouse_Product   p = productsTable.getSelectionModel().getSelectedItem();
        if (w == null || p == null) { Warehouse_DialogUtil.info("No selection", "Select a warehouse and a product."); return; }

        if (Warehouse_DialogUtil.confirm("Delete product?", "Are you sure you want to delete this product?")) {
            try {
                warehouseService.deleteProductFromDb(Integer.parseInt(p.getId()), Integer.parseInt(w.getId()));
            } catch (NumberFormatException ignored) {}
            w.getProducts().remove(p);
            currentProducts.remove(p);
            refreshComputedUI();
        }
    }

    @FXML private void onResetData() {
        if (Warehouse_DialogUtil.confirm("Reset data?", "Reload data from the database?")) {
            warehouses.setAll(warehouseService.loadWarehousesFromDb());
            refreshComputedUI();
            refreshDeliveries();
            if (!warehouses.isEmpty()) warehousesTable.getSelectionModel().select(0);
            Warehouse_DialogUtil.info("Reset", "Data has been reloaded from the database.");
        }
    }

    @FXML private void onDownloadReport() {
        try {
            String fileName = "InventoryReport_" + java.time.LocalDate.now() + ".pdf";
            String filePath = System.getProperty("user.dir") + java.io.File.separator + fileName;

            new Warehouse_ReportService().generateReport(filePath, warehouses,
                    new java.util.ArrayList<>(warehouse_LowStockRows),
                    warehouseService.getTotalStock(warehouses),
                    warehouse_LowStockRows.size(),
                    warehouseService.getCapacityUsagePercent(warehouses));

            Warehouse_DialogUtil.info("Report Downloaded", "PDF saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            Warehouse_DialogUtil.info("Error", "Could not generate PDF: " + e.getMessage());
        }
    }

    private void refreshDeliveries() {
        deliveries.setAll(warehouse_DeliveryService.loadPendingDeliveries());
    }

    @FXML private void onRefreshPendingOrders() { refreshPendingOrders(); }

    private void refreshPendingOrders() {
        pendingOrders.setAll(orderDAO.getOrdersByStatus("Processing"));
    }

    private void wirePendingOrdersColumns() {
        poIdCol.setCellValueFactory(c -> c.getValue().orderIdProperty().asObject());
        poCustomerCol.setCellValueFactory(c -> c.getValue().customerProperty());
        poDateCol.setCellValueFactory(c -> c.getValue().orderDateProperty());
        poTotalCol.setCellValueFactory(c -> c.getValue().totalProperty().asObject());
        poItemsCol.setCellValueFactory(c -> c.getValue().itemCountProperty().asObject());

        poTotalCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("£%.2f", v));
            }
        });

        poPackCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button packBtn = new javafx.scene.control.Button("✔ Pack");
            {
                packBtn.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-padding:5 12;-fx-background-radius:5;-fx-cursor:hand;");
                packBtn.setOnAction(e -> {
                    Order o = getTableRow().getItem();
                    if (o != null && orderDAO.updateOrderStatus(o.getOrderId(), "Picking")) {
                        Warehouse_DialogUtil.info("Order Packed ✅",
                                "Order #" + o.getOrderId() + " is now 'Picking'.\n" +
                                "It will appear in the Delivery dashboard.");
                        refreshPendingOrders();
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : packBtn);
            }
        });
    }

    private void refreshComputedUI() {
        warehouse_LowStockRows.setAll(warehouseService.getLowStockRows(warehouses));
        boolean showLowStock = !warehouse_LowStockRows.isEmpty();
        lowStockCard.setVisible(showLowStock);
        lowStockCard.setManaged(showLowStock);

        totalStockLabel.setText(warehouseService.getTotalStock(warehouses) + " units");
        lowStockCountLabel.setText(String.valueOf(warehouse_LowStockRows.size()));
        capacityUsageLabel.setText(warehouseService.getCapacityUsagePercent(warehouses) + "%");
        warehousesTable.refresh();
    }

    private Stage buildDialog(String title, FXMLLoader loader, int w, int h) throws Exception {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initOwner(toggleWarehousesBtn.getScene().getWindow());
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(new Scene(loader.load(), w, h));
        return dialogStage;
    }
}