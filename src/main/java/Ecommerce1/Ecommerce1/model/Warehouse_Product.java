package Ecommerce1.Ecommerce1.model;

import javafx.beans.property.*;

/**
 * Product model — encapsulates all product data with JavaFX properties.
 * Standalone class (no longer nested inside StaffDashboardController).
 *
 * OOP principles applied:
 *  - Encapsulation: all fields private, exposed via getters/setters
 *  - JavaFX properties for UI binding
 */
public class Warehouse_Product {

    private final StringProperty  id           = new SimpleStringProperty();
    private final StringProperty  name         = new SimpleStringProperty();
    private final StringProperty  sku          = new SimpleStringProperty();
    private final IntegerProperty quantity     = new SimpleIntegerProperty();
    private final IntegerProperty minThreshold = new SimpleIntegerProperty();
    private final IntegerProperty reorderLevel = new SimpleIntegerProperty();
    private final StringProperty  lastRestock  = new SimpleStringProperty();

    public Warehouse_Product(String id, String name, String sku,
                   int quantity, int minThreshold, int reorderLevel, String lastRestock) {
        this.id.set(id);
        this.name.set(name);
        this.sku.set(sku);
        this.quantity.set(quantity);
        this.minThreshold.set(minThreshold);
        this.reorderLevel.set(reorderLevel);
        this.lastRestock.set(lastRestock);
    }

    // ── Getters ──
    public String getId()           { return id.get(); }
    public String getName()         { return name.get(); }
    public String getSku()          { return sku.get(); }
    public int    getQuantity()     { return quantity.get(); }
    public int    getMinThreshold() { return minThreshold.get(); }
    public int    getReorderLevel() { return reorderLevel.get(); }
    public String getLastRestock()  { return lastRestock.get(); }

    // ── Setters ──
    public void setName(String name)               { this.name.set(name); }
    public void setSku(String sku)                 { this.sku.set(sku); }
    public void setQuantity(int quantity)          { this.quantity.set(quantity); }
    public void setMinThreshold(int minThreshold)  { this.minThreshold.set(minThreshold); }
    public void setReorderLevel(int reorderLevel)  { this.reorderLevel.set(reorderLevel); }
    public void setLastRestock(String lastRestock) { this.lastRestock.set(lastRestock); }

    // ── JavaFX Properties (for TableView binding) ──
    public StringProperty  idProperty()           { return id; }
    public StringProperty  nameProperty()         { return name; }
    public StringProperty  skuProperty()          { return sku; }
    public IntegerProperty quantityProperty()     { return quantity; }
    public IntegerProperty minThresholdProperty() { return minThreshold; }
    public IntegerProperty reorderLevelProperty() { return reorderLevel; }
    public StringProperty  lastRestockProperty()  { return lastRestock; }

    // ── Utility ──
    public boolean isLowStock() {
        return quantity.get() < minThreshold.get();
    }

    @Override
    public String toString() {
        return name.get() + " (" + sku.get() + ")";
    }
}
