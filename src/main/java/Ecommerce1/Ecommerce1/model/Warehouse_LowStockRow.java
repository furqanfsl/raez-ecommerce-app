package Ecommerce1.Ecommerce1.model;

import javafx.beans.property.*;

/**
 * LowStockRow model — represents a row in the urgent stock alert table.
 *
 * OOP principles applied:
 *  - Encapsulation: private fields with property accessors
 *  - Single Responsibility: only holds low-stock display data
 */
public class Warehouse_LowStockRow {

    private final StringProperty  productName       = new SimpleStringProperty();
    private final StringProperty  sku               = new SimpleStringProperty();
    private final IntegerProperty qty               = new SimpleIntegerProperty();
    private final StringProperty  warehouseLocation = new SimpleStringProperty();

    public Warehouse_LowStockRow(String productName, String sku, int qty, String warehouseLocation) {
        this.productName.set(productName);
        this.sku.set(sku);
        this.qty.set(qty);
        this.warehouseLocation.set(warehouseLocation);
    }

    public String getProductName()       { return productName.get(); }
    public String getSku()               { return sku.get(); }
    public int    getQty()               { return qty.get(); }
    public String getWarehouseLocation() { return warehouseLocation.get(); }

    public StringProperty  productNameProperty()       { return productName; }
    public StringProperty  skuProperty()               { return sku; }
    public IntegerProperty qtyProperty()               { return qty; }
    public StringProperty  warehouseLocationProperty() { return warehouseLocation; }
}
