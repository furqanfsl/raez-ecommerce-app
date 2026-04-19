package com.reaz.customer.model;

public class CustomerOrder {

    private int    orderId;
    private String orderDate;
    private String status;
    private double totalAmount;
    private String productName;

    public CustomerOrder() {}

    public CustomerOrder(int orderId, String orderDate, String status,
                         double totalAmount, String productName) {
        this.orderId     = orderId;
        this.orderDate   = orderDate;
        this.status      = status;
        this.totalAmount = totalAmount;
        this.productName = productName;
    }

    public int    getOrderId()                         { return orderId;          }
    public void   setOrderId(int id)                   { this.orderId = id;       }

    public String getOrderDate()                       { return orderDate;        }
    public void   setOrderDate(String d)               { this.orderDate = d;      }

    public String getStatus()                          { return status;           }
    public void   setStatus(String s)                  { this.status = s;         }

    public double getTotalAmount()                     { return totalAmount;      }
    public void   setTotalAmount(double a)             { this.totalAmount = a;    }

    public String getProductName()                     { return productName;      }
    public void   setProductName(String p)             { this.productName = p;    }

    public String getFormattedAmount() {
        return "£" + String.format("%,.2f", totalAmount);
    }
}
