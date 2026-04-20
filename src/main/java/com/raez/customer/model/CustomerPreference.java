package com.raez.customer.model;

public class CustomerPreference {

    private int    preferenceId;
    private int    customerId;
    private String preferredCategories;
    private String notificationSettings;
    private String deliveryInstructions;

    public CustomerPreference() {}

    public int    getPreferenceId()                            { return preferenceId;              }
    public void   setPreferenceId(int id)                      { this.preferenceId = id;           }

    public int    getCustomerId()                              { return customerId;                }
    public void   setCustomerId(int id)                        { this.customerId = id;             }

    public String getPreferredCategories()                     { return preferredCategories;       }
    public void   setPreferredCategories(String c)             { this.preferredCategories = c;     }

    public String getNotificationSettings()                    { return notificationSettings;      }
    public void   setNotificationSettings(String s)            { this.notificationSettings = s;    }

    public String getDeliveryInstructions()                    { return deliveryInstructions;      }
    public void   setDeliveryInstructions(String d)            { this.deliveryInstructions = d;    }
}
