package com.reaz.delivery.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DeliveryDelivery {

    private final StringProperty deliveryId;
    private final StringProperty orderId;
    private final StringProperty customerAddress;
    private final StringProperty orderStatus;
    private final StringProperty orderDate;
    private final StringProperty numOfItems;
    private final StringProperty driverId;

    public DeliveryDelivery(String deliveryId, String orderId, String customerAddress,
                            String orderStatus, String orderDate, String numOfItems, String driverId) {
        this.deliveryId      = new SimpleStringProperty(deliveryId);
        this.orderId         = new SimpleStringProperty(orderId);
        this.customerAddress = new SimpleStringProperty(customerAddress);
        this.orderStatus     = new SimpleStringProperty(orderStatus);
        this.orderDate       = new SimpleStringProperty(orderDate);
        this.numOfItems      = new SimpleStringProperty(numOfItems);
        this.driverId        = new SimpleStringProperty(driverId);
    }

    public String getDeliveryId()        { return deliveryId.get(); }
    public StringProperty deliveryIdProperty()      { return deliveryId; }

    public String getOrderId()           { return orderId.get(); }
    public StringProperty orderIdProperty()         { return orderId; }

    public String getCustomerAddress()   { return customerAddress.get(); }
    public StringProperty customerAddressProperty() { return customerAddress; }

    public String getOrderStatus()       { return orderStatus.get(); }
    public StringProperty orderStatusProperty()     { return orderStatus; }

    public String getOrderDate()         { return orderDate.get(); }
    public StringProperty orderDateProperty()       { return orderDate; }

    public String getNumOfItems()        { return numOfItems.get(); }
    public StringProperty numOfItemsProperty()      { return numOfItems; }

    public String getDriverId()          { return driverId.get(); }
    public StringProperty driverIdProperty()        { return driverId; }
}
