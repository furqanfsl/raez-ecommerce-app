package Ecommerce1.Ecommerce1;

import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import Ecommerce1.Ecommerce1.util.Warehouse_ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Set;

/**
 * AddWarehouseDialogController — handles the Add Warehouse dialog UI.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog UI
 *  - Encapsulation: Result is an immutable data holder
 */
public class AddWarehouseDialogController {

    @FXML private TextField warehouseIdField;
    @FXML private TextField nameField;
    @FXML private TextField locationField;
    @FXML private TextField productLineField;
    @FXML private TextField maxCapacityField;
    @FXML private Label     errorLabel;
    @FXML private Label     warehouseIdErrorLabel;

    private Stage       stage;
    private Set<String> existingWarehouseIds;
    private Result      result;

    /**
     * Result — immutable data holder for dialog output.
     */
    public static class Result {
        public final String warehouseId;
        public final String name;
        public final String location;
        public final String productLine;
        public final int    maxCapacity;

        public Result(String warehouseId, String name, String location,
                      String productLine, int maxCapacity) {
            this.warehouseId = warehouseId;
            this.name        = name;
            this.location    = location;
            this.productLine = productLine;
            this.maxCapacity = maxCapacity;
        }
    }

    public void setStage(Stage stage)                          { this.stage = stage; }
    public void setExistingWarehouseIds(Set<String> ids)       { this.existingWarehouseIds = ids; }
    public Result getResult()                                  { return result; }

    @FXML private void onClose() { result = null; stage.close(); }

    @FXML
    private void onSubmit() {
        hideErrors();

        String warehouseId   = Warehouse_ValidationUtil.safe(warehouseIdField.getText());
        String name          = Warehouse_ValidationUtil.safe(nameField.getText());
        String location      = Warehouse_ValidationUtil.safe(locationField.getText());
        String productLine   = Warehouse_ValidationUtil.safe(productLineField.getText());
        String maxCapacityStr = Warehouse_ValidationUtil.safe(maxCapacityField.getText());

        if (warehouseId.isEmpty() || name.isEmpty() || location.isEmpty()
                || maxCapacityStr.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "All fields are required.");
            return;
        }

        if (existingWarehouseIds != null && existingWarehouseIds.contains(warehouseId)) {
            Warehouse_DialogUtil.showError(warehouseIdErrorLabel, "Warehouse ID already exists.");
            return;
        }

        int maxCapacity = Warehouse_ValidationUtil.parsePositive(maxCapacityStr);
        if (maxCapacity < 0) {
            Warehouse_DialogUtil.showError(errorLabel, "Maximum capacity must be a positive number.");
            return;
        }

        result = new Result(warehouseId, name, location, productLine, maxCapacity);
        stage.close();
    }

    private void hideErrors() {
        Warehouse_DialogUtil.hideError(errorLabel);
        Warehouse_DialogUtil.hideError(warehouseIdErrorLabel);
    }
}