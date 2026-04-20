package com.reaz.orders.model;

import javafx.beans.property.*;

public class Order {
    private final IntegerProperty orderId    = new SimpleIntegerProperty();
    private final StringProperty  customer   = new SimpleStringProperty();
    private final StringProperty  orderDate  = new SimpleStringProperty();
    private final DoubleProperty  total      = new SimpleDoubleProperty();
    private final StringProperty  status     = new SimpleStringProperty();
    private final IntegerProperty itemCount  = new SimpleIntegerProperty();

    public Order(int orderId, String customer, String orderDate, double total, String status, int itemCount) {
        this.orderId.set(orderId);
        this.customer.set(customer);
        this.orderDate.set(orderDate);
        this.total.set(total);
        this.status.set(status);
        this.itemCount.set(itemCount);
    }

    public int    getOrderId()   { return orderId.get(); }
    public String getCustomer()  { return customer.get(); }
    public String getOrderDate() { return orderDate.get(); }
    public double getTotal()     { return total.get(); }
    public String getStatus()    { return status.get(); }
    public int    getItemCount() { return itemCount.get(); }

    public IntegerProperty orderIdProperty()   { return orderId; }
    public StringProperty  customerProperty()  { return customer; }
    public StringProperty  orderDateProperty() { return orderDate; }
    public DoubleProperty  totalProperty()     { return total; }
    public StringProperty  statusProperty()    { return status; }
    public IntegerProperty itemCountProperty() { return itemCount; }
}
