package Ecommerce1.Ecommerce1;

import Ecommerce1.Ecommerce1.model.Warehouse;
import Ecommerce1.Ecommerce1.util.Warehouse_DialogUtil;
import Ecommerce1.Ecommerce1.util.Warehouse_ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Set;

/**
 * EditWarehouseDialogController — handles the Edit Warehouse dialog UI.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog UI
 *  - Uses standalone Warehouse model
 */
public class EditWarehouseDialogController {

    @FXML private TextField warehouseIdField;
    @FXML private TextField nameField;
    @FXML private TextField locationField;
    @FXML private TextField productLineField;
    @FXML private TextField maxCapacityField;
    @FXML private Label     errorLabel;

    private Stage       stage;
    private Warehouse   warehouse;
    private Set<String> existingWarehouseIds;
    private Warehouse   result;

    public void setStage(Stage stage)                    { this.stage = stage; }
    public void setExistingWarehouseIds(Set<String> ids) { this.existingWarehouseIds = ids; }
    public Warehouse getResult()                         { return result; }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        warehouseIdField.setText(warehouse.getWarehouseId());
        nameField.setText(warehouse.getName());
        locationField.setText(warehouse.getLocation());
        productLineField.setText(warehouse.getProductLine());
        maxCapacityField.setText(String.valueOf(warehouse.getMaxCapacity()));
    }

    @FXML private void onClose() { result = null; stage.close(); }

    @FXML
    private void onSubmit() {
        Warehouse_DialogUtil.hideError(errorLabel);

        String warehouseId    = Warehouse_ValidationUtil.safe(warehouseIdField.getText());
        String name           = Warehouse_ValidationUtil.safe(nameField.getText());
        String location       = Warehouse_ValidationUtil.safe(locationField.getText());
        String productLine    = Warehouse_ValidationUtil.safe(productLineField.getText());
        String maxCapacityStr = Warehouse_ValidationUtil.safe(maxCapacityField.getText());

        if (warehouseId.isEmpty() || name.isEmpty() || location.isEmpty()
                || productLine.isEmpty() || maxCapacityStr.isEmpty()) {
            Warehouse_DialogUtil.showError(errorLabel, "All fields are required.");
            return;
        }

        if (existingWarehouseIds != null
                && existingWarehouseIds.contains(warehouseId)
                && !warehouseId.equals(warehouse.getWarehouseId())) {
            Warehouse_DialogUtil.showError(errorLabel, "Warehouse ID must be unique.");
            return;
        }

        int capacity = Warehouse_ValidationUtil.parsePositive(maxCapacityStr);
        if (capacity < 0) {
            Warehouse_DialogUtil.showError(errorLabel, "Maximum capacity must be a positive number.");
            return;
        }

        int currentStock = warehouse.getCurrentStock();
        if (capacity < currentStock) {
            Warehouse_DialogUtil.showError(errorLabel,
                    "Maximum capacity cannot be less than current stock (" + currentStock + " units).");
            return;
        }

        Warehouse updated = new Warehouse(
                warehouse.getId(), warehouseId, name, location, productLine, capacity);
        updated.getProducts().setAll(warehouse.getProducts());
        result = updated;
        stage.close();
    }
}