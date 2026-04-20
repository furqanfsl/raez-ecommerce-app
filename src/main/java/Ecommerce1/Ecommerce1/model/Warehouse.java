package Ecommerce1.Ecommerce1.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Warehouse model — encapsulates all warehouse data with JavaFX properties.
 * Standalone class (no longer nested inside StaffDashboardController).
 *
 * OOP principles applied:
 *  - Encapsulation: all fields private, exposed via getters/setters
 *  - Composition: contains a list of Products
 */
public class Warehouse {

    private final StringProperty  id           = new SimpleStringProperty();
    private final StringProperty  warehouseId  = new SimpleStringProperty();
    private final StringProperty  name         = new SimpleStringProperty();
    private final StringProperty  location     = new SimpleStringProperty();
    private final StringProperty  productLine  = new SimpleStringProperty();
    private final IntegerProperty maxCapacity  = new SimpleIntegerProperty();
    private final ObservableList<Warehouse_Product> warehouse_Products = FXCollections.observableArrayList();

    public Warehouse(String id, String warehouseId, String name,
                     String location, String productLine, int maxCapacity) {
        this.id.set(id);
        this.warehouseId.set(warehouseId);
        this.name.set(name);
        this.location.set(location);
        this.productLine.set(productLine);
        this.maxCapacity.set(maxCapacity);
    }

    // ── Getters ──
    public String getId()          { return id.get(); }
    public String getWarehouseId() { return warehouseId.get(); }
    public String getName()        { return name.get(); }
    public String getLocation()    { return location.get(); }
    public String getProductLine() { return productLine.get(); }
    public int    getMaxCapacity() { return maxCapacity.get(); }
    public ObservableList<Warehouse_Product> getProducts() { return warehouse_Products; }

    // ── Setters ──
    public void setWarehouseId(String warehouseId) { this.warehouseId.set(warehouseId); }
    public void setName(String name)               { this.name.set(name); }
    public void setLocation(String location)       { this.location.set(location); }
    public void setProductLine(String productLine) { this.productLine.set(productLine); }
    public void setMaxCapacity(int maxCapacity)    { this.maxCapacity.set(maxCapacity); }

    // ── JavaFX Properties (for TableView binding) ──
    public StringProperty  idProperty()          { return id; }
    public StringProperty  warehouseIdProperty() { return warehouseId; }
    public StringProperty  nameProperty()        { return name; }
    public StringProperty  locationProperty()    { return location; }
    public StringProperty  productLineProperty() { return productLine; }
    public IntegerProperty maxCapacityProperty() { return maxCapacity; }

    // ── Utility ──
    public int getCurrentStock() {
        return warehouse_Products.stream().mapToInt(Warehouse_Product::getQuantity).sum();
    }

    public int getAvailableSpace() {
        return maxCapacity.get() - getCurrentStock();
    }

    public int getLowStockCount() {
        return (int) warehouse_Products.stream().filter(Warehouse_Product::isLowStock).count();
    }

    public int getCapacityPercentage() {
        return maxCapacity.get() == 0 ? 0
                : Math.round((getCurrentStock() * 100f) / maxCapacity.get());
    }

    public String getFormattedCapacity() {
        return getCurrentStock() + " / " + maxCapacity.get()
                + " (" + getCapacityPercentage() + "%)";
    }

    public boolean hasProductWithSku(String sku) {
        return warehouse_Products.stream().anyMatch(p -> p.getSku().equalsIgnoreCase(sku));
    }

    @Override
    public String toString() {
        return name.get() + " - " + location.get();
    }
}
