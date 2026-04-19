package com.reaz.model;

public class User {

    public int     userID;
    public String  firstName;
    public String  lastName;
    public String  email;
    public String  roleName;
    public int     isActive;
    public String  username;
    public String  lastLogin;

    public User() {}

    public User(int userID, String firstName, String lastName, String email,
                String roleName, int isActive, String username, String lastLogin) {
        this.userID    = userID;
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email;
        this.roleName  = roleName;
        this.isActive  = isActive;
        this.username  = username;
        this.lastLogin = lastLogin;
    }

    /** True for staff roles that may access the admin dashboard (demo). */
    public boolean isAdmin() {
        if (roleName == null) return false;
        String r = roleName.toLowerCase();
        return r.contains("admin") || r.equals("super_admin");
    }

    @Override
    public String toString() {
        return firstName + " (" + roleName + ")";
    }
}
