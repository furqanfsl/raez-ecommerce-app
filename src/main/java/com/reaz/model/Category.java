package com.reaz.model;

/**
 * Matches the 'category' table.
 */
public class Category {

    public int    id;
    public String name;
    public String description;
    public int    parentId;
    public String status;

    public Category() {}

    public Category(String name, String description, String status) {
        this.name        = name;
        this.description = description;
        this.status      = status;
    }

    @Override
    public String toString() {
        return name;
    }
}