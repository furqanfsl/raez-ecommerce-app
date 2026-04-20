package com.raez.reviews.util;

/** No-op: master DBConnection already applies raez_unified_schema.sql which includes all reviews tables. */
public class DatabaseInitializer {

    public DatabaseInitializer(DatabaseManager ignored) {}

    public void initialize() {
        // Master DBConnection rebuilds the full schema on startup — nothing to do here.
    }
}
