package Ecommerce1.Ecommerce1;

import Ecommerce1.Ecommerce1.model.Warehouse_Product;
import Ecommerce1.Ecommerce1.model.Warehouse;
import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import Ecommerce1.Ecommerce1.util.Warehouse_ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * EditProductDialogController — handles the Edit Product dialog UI.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog UI
 *  - Encapsulation: Result is an immutable data holder
 *  - Uses standalone Product/Warehouse models
 */
public class Warehouse_EditProductDialogController {

    @FXML private TextField nameField;
    @FXML private TextField skuField;
    @FXML private TextField quantityField;
    @FXML private TextField minThresholdField;
    @FXML private TextField reorderLevelField;
    @FXML private Label     errorLabel;

    private Stage     stage;
    private Warehouse warehouse;
    private Warehouse_Product   warehouse_Product;
    private Result    result;

    /**
     * Result — immutable data holder for dialog output.
     */
    public static class Result {
        public final String name;
        public final String sku;
        public final int    quantity;
        public final int    minThreshold;
        public final int    reorderLevel;

        public Result(String name, String sku, int quantity, int minThreshold, int reorderLevel) {
            this.name         = name;
            this.sku          = sku;
            this.quantity     = quantity;
            this.minThreshold = minThreshold;
            this.reorderLevel = reorderLevel;
        }
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public Result getResult()         { return result; }

    public void setData(Warehouse warehouse, Warehouse_Product warehouse_Product) {
        this.warehouse = warehouse;
        this.warehouse_Product   = warehouse_Product;

        nameField.setText(warehouse_Product.getName());
        skuField.setText(warehouse_Product.getSku());
        quantityField.setText(String.valueOf(warehouse_Product.getQuantity()));
        minThresholdField.setText(String.valueOf(warehouse_Product.getMinThreshold()));
        reorderLevelField.setText(String.valueOf(warehouse_Product.getReorderLevel()));
    }

    @FXML private void onClose() { result = null; stage.close(); }

    @FXML
    private void onSubmit() {
        Warehouse_DialogUtil.hideError(errorLabel);

        String name      = Warehouse_ValidationUtil.safe(nameField.getText());
        String sku       = Warehouse_ValidationUtil.safe(skuField.getText());
        String qtyStr    = Warehouse_ValidationUtil.safe(quantityField.getText());
        String minStr    = Warehouse_ValidationUtil.safe(minThresholdField.getText());
        String reorderStr = Warehouse_ValidationUtil.safe(reorderLevelField.getText());

        if (name.isEmpty() || sku.isEmpty() || qtyStr.isEmpty()
                || minStr.isEmpty() || reorderStr.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "All fields are required.");
            return;
        }

        int qty = Warehouse_ValidationUtil.parseNonNegative(qtyStr);
        if (qty < 0) { Warehouse_DialogUtil.showError(errorLabel, "Quantity must be a non-negative number."); return; }

        int min = Warehouse_ValidationUtil.parseNonNegative(minStr);
        if (min < 0) { Warehouse_DialogUtil.showError(errorLabel, "Minimum threshold must be a non-negative number."); return; }

        int reorder = Warehouse_ValidationUtil.parseNonNegative(reorderStr);
        if (reorder < 0) { Warehouse_DialogUtil.showError(errorLabel, "Reorder level must be a non-negative number."); return; }

        result = new Result(name, sku, qty, min, reorder);
        stage.close();
    }
}