package Ecommerce1.Ecommerce1;

import Ecommerce1.Ecommerce1.model.Warehouse_Product;
import Ecommerce1.Ecommerce1.model.Warehouse;
import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import Ecommerce1.Ecommerce1.util.Warehouse_ValidationUtil;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.stream.Collectors;

/**
 * TransferStockDialogController — handles the Transfer Stock dialog UI.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog UI
 *  - Encapsulation: Result is an immutable data holder
 *  - Uses standalone Product/Warehouse models
 */
public class Warehouse_TransferStockDialogController {

    @FXML private ComboBox<Warehouse> fromWarehouseCombo;
    @FXML private ComboBox<Warehouse> toWarehouseCombo;
    @FXML private ComboBox<Warehouse_Product>   productCombo;
    @FXML private TextField           quantityField;
    @FXML private Label               availableLabel;
    @FXML private Label               errorLabel;

    private Stage  stage;
    private Result result;

    /**
     * Result — immutable data holder for dialog output.
     */
    public static class Result {
        public final Warehouse from;
        public final Warehouse to;
        public final Warehouse_Product   warehouse_Product;
        public final int       quantity;

        public Result(Warehouse from, Warehouse to, Warehouse_Product warehouse_Product, int quantity) {
            this.from     = from;
            this.to       = to;
            this.warehouse_Product  = warehouse_Product;
            this.quantity = quantity;
        }
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public Result getResult()         { return result; }

    public void setWarehouses(ObservableList<Warehouse> warehouses) {
        fromWarehouseCombo.getItems().setAll(warehouses);
        setupWarehouseCell(fromWarehouseCombo);
        setupWarehouseCell(toWarehouseCombo);

        fromWarehouseCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldW, newW) -> {
                    productCombo.getItems().clear();
                    productCombo.getSelectionModel().clearSelection();
                    quantityField.clear();
                    Warehouse_DialogUtil.hideError(errorLabel);
                    Warehouse_DialogUtil.hideError(availableLabel);

                    if (newW != null) {
                        toWarehouseCombo.getItems().setAll(
                                warehouses.stream()
                                        .filter(w -> w != newW)
                                        .collect(Collectors.toList())
                        );
                        productCombo.getItems().setAll(newW.getProducts());
                        setupProductCell(productCombo);
                    } else {
                        toWarehouseCombo.getItems().clear();
                    }
                });

        productCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldP, newP) -> {
                    if (newP != null) {
                        availableLabel.setText("Available: " + newP.getQuantity() + " units");
                        availableLabel.setManaged(true);
                        availableLabel.setVisible(true);
                    } else {
                        Warehouse_DialogUtil.hideError(availableLabel);
                    }
                });
    }

    @FXML private void onClose() { result = null; stage.close(); }

    @FXML
    private void onSubmit() {
        Warehouse_DialogUtil.hideError(errorLabel);

        Warehouse from    = fromWarehouseCombo.getSelectionModel().getSelectedItem();
        Warehouse to      = toWarehouseCombo.getSelectionModel().getSelectedItem();
        Warehouse_Product   warehouse_Product = productCombo.getSelectionModel().getSelectedItem();
        String    qtyStr  = Warehouse_ValidationUtil.safe(quantityField.getText());

        if (from == null || to == null || warehouse_Product == null || qtyStr.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "All fields are required.");
            return;
        }
        if (from == to) {
            Warehouse_DialogUtil.showError(errorLabel, "Cannot transfer to the same warehouse.");
            return;
        }

        int qty = Warehouse_ValidationUtil.parsePositive(qtyStr);
        if (qty < 0) {
            Warehouse_DialogUtil.showError(errorLabel, "Quantity must be a positive number.");
            return;
        }
        if (qty > warehouse_Product.getQuantity()) {
            Warehouse_DialogUtil.showError(errorLabel,
                    "Insufficient stock. Available: " + warehouse_Product.getQuantity() + " units.");
            return;
        }
        if (to.getAvailableSpace() < qty) {
            Warehouse_DialogUtil.showError(errorLabel,
                    "Transfer would exceed destination warehouse capacity. "
                    + "Available space: " + to.getAvailableSpace() + " units.");
            return;
        }

        result = new Result(from, to, warehouse_Product, qty);
        stage.close();
    }

    private void setupWarehouseCell(ComboBox<Warehouse> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Warehouse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getLocation());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Warehouse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getLocation());
            }
        });
    }

    private void setupProductCell(ComboBox<Warehouse_Product> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Warehouse_Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (" + item.getSku() + ") - Available: " + item.getQuantity());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Warehouse_Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (" + item.getSku() + ") - Available: " + item.getQuantity());
            }
        });
    }
}