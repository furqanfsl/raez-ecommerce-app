package Ecommerce1.Ecommerce1;

import Ecommerce1.Ecommerce1.model.Warehouse_Product;
import Ecommerce1.Ecommerce1.model.Warehouse;
import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import Ecommerce1.Ecommerce1.util.Warehouse_ValidationUtil;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

/**
 * AddProductDialogController — handles the Add Product dialog UI.
 * Validation delegated to ValidationUtil.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog UI
 *  - Encapsulation: Result is an immutable data holder
 *  - Uses standalone Product/Warehouse models
 */
public class Warehouse_AddProductDialogController {

    @FXML private ComboBox<Warehouse> warehouseCombo;
    @FXML private TextField           nameField;
    @FXML private TextField           skuField;
    @FXML private TextField           quantityField;
    @FXML private TextField           minThresholdField;
    @FXML private TextField           reorderLevelField;
    @FXML private Label               errorLabel;

    private Stage  stage;
    private Result result;

    /**
     * Result — immutable data holder for dialog output.
     */
    public static class Result {
        public final Warehouse warehouse;
        public final String    name;
        public final String    sku;
        public final int       quantity;
        public final int       minThreshold;
        public final int       reorderLevel;
        public final String    lastRestock;

        public Result(Warehouse warehouse, String name, String sku,
                      int quantity, int minThreshold, int reorderLevel, String lastRestock) {
            this.warehouse    = warehouse;
            this.name         = name;
            this.sku          = sku;
            this.quantity     = quantity;
            this.minThreshold = minThreshold;
            this.reorderLevel = reorderLevel;
            this.lastRestock  = lastRestock;
        }
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public Result getResult()         { return result; }

    public void setWarehouses(ObservableList<Warehouse> warehouses, Warehouse preselected) {
        warehouseCombo.getItems().setAll(warehouses);
        warehouseCombo.setCellFactory(cb -> warehouseCell());
        warehouseCombo.setButtonCell(warehouseCell());
        if (preselected != null) warehouseCombo.getSelectionModel().select(preselected);
    }

    @FXML private void onClose() { result = null; stage.close(); }

    @FXML
    private void onSubmit() {
        Warehouse_DialogUtil.hideError(errorLabel);

        Warehouse w      = warehouseCombo.getSelectionModel().getSelectedItem();
        String name      = Warehouse_ValidationUtil.safe(nameField.getText());
        String sku       = Warehouse_ValidationUtil.safe(skuField.getText());
        String qtyStr    = Warehouse_ValidationUtil.safe(quantityField.getText());
        String minStr    = Warehouse_ValidationUtil.safe(minThresholdField.getText());
        String reoStr    = Warehouse_ValidationUtil.safe(reorderLevelField.getText());

        if (w == null || name.isEmpty() || sku.isEmpty()
                || qtyStr.isEmpty() || minStr.isEmpty() || reoStr.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "All fields are required.");
            return;
        }

        int qty = Warehouse_ValidationUtil.parseNonNegative(qtyStr);
        if (qty < 0) { Warehouse_DialogUtil.showError(errorLabel, "Quantity must be a non-negative number."); return; }

        int min = Warehouse_ValidationUtil.parseNonNegative(minStr);
        if (min < 0) { Warehouse_DialogUtil.showError(errorLabel, "Minimum threshold must be a non-negative number."); return; }

        int reo = Warehouse_ValidationUtil.parseNonNegative(reoStr);
        if (reo < 0) { Warehouse_DialogUtil.showError(errorLabel, "Reorder level must be a non-negative number."); return; }

        if (w.hasProductWithSku(sku)) {
            Warehouse_DialogUtil.showError(errorLabel, "A product with this SKU already exists in the selected warehouse.");
            return;
        }

        if (w.getMaxCapacity() > 0 && w.getAvailableSpace() < qty) {
            Warehouse_DialogUtil.showError(errorLabel, "Adding this product would exceed warehouse capacity. "
                    + "Available space: " + w.getAvailableSpace() + " units.");
            return;
        }
        
        result = new Result(w, name, sku, qty, min, reo, LocalDate.now().toString());
        stage.close();
    }

    private ListCell<Warehouse> warehouseCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Warehouse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getLocation());
            }
        };
    }
}