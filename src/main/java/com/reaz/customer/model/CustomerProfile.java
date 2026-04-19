package com.reaz.customer.model;

public class CustomerProfile {

    private int    customerId;
    private int    userId;
    private String phone;
    private String address;
    private String idCardPath;

    public CustomerProfile() {}

    public CustomerProfile(int customerId, int userId, String phone,
                           String address, String idCardPath) {
        this.customerId = customerId;
        this.userId     = userId;
        this.phone      = phone;
        this.address    = address;
        this.idCardPath = idCardPath;
    }

    public int    getCustomerId()                     { return customerId;      }
    public void   setCustomerId(int id)               { this.customerId = id;   }

    public int    getUserId()                         { return userId;          }
    public void   setUserId(int id)                   { this.userId = id;       }

    public String getPhone()                          { return phone;           }
    public void   setPhone(String phone)              { this.phone = phone;     }

    public String getAddress()                        { return address;         }
    public void   setAddress(String address)          { this.address = address; }

    public String getIdCardPath()                     { return idCardPath;      }
    public void   setIdCardPath(String path)          { this.idCardPath = path; }

    public String getIdCardFileName() {
        if (idCardPath == null || idCardPath.isBlank()) return "No ID card uploaded";
        return new java.io.File(idCardPath).getName();
    }
}
