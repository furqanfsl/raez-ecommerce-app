package models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Order {

    private final StringProperty id;
    private final StringProperty customer;
    private final StringProperty address;
    private final StringProperty status;
    private final StringProperty driverId;
    private final StringProperty items;
    private final StringProperty date;
    private final StringProperty priority;

    public Order(String id, String customer, String address, String status,
                 String driverId, String items, String date, String priority) {
        this.id = new SimpleStringProperty(id);
        this.customer = new SimpleStringProperty(customer);
        this.address = new SimpleStringProperty(address);
        this.status = new SimpleStringProperty(status);
        this.driverId = new SimpleStringProperty(driverId);
        this.items = new SimpleStringProperty(items);
        this.date = new SimpleStringProperty(date);
        this.priority = new SimpleStringProperty(priority);
    }

    public String getId() {
        return id.get();
    }

    public void setId(String value) {
        id.set(value);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getCustomer() {
        return customer.get();
    }

    public void setCustomer(String value) {
        customer.set(value);
    }

    public StringProperty customerProperty() {
        return customer;
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public StringProperty addressProperty() {
        return address;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getDriverId() {
        return driverId.get();
    }

    public void setDriverId(String value) {
        driverId.set(value);
    }

    public StringProperty driverIdProperty() {
        return driverId;
    }

    public String getItems() {
        return items.get();
    }

    public void setItems(String value) {
        items.set(value);
    }

    public StringProperty itemsProperty() {
        return items;
    }

    public String getDate() {
        return date.get();
    }

    public void setDate(String value) {
        date.set(value);
    }

    public StringProperty dateProperty() {
        return date;
    }

    public String getPriority() {
        return priority.get();
    }

    public void setPriority(String value) {
        priority.set(value);
    }

    public StringProperty priorityProperty() {
        return priority;
    }
}