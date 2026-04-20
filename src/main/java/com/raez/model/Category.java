package com.raez.model;

/**
 * Matches the unified {@code categories} table.
 */
public class Category {

    public int    categoryID;
    public String categoryName;
    public String description;
    public int    parentID;
    /** 1 = active listing, 0 = inactive */
    public int    isActive;

    public Category() {}

    public Category(String categoryName, String description, int isActive) {
        this.categoryName = categoryName;
        this.description  = description;
        this.isActive      = isActive;
    }

    @Override
    public String toString() {
        return categoryName;
    }
}
