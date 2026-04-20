package com.raez.reviews.util;

import com.raez.db.DBConnection;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** Delegates to the master DBConnection singleton (unified raez.db). */
public class DatabaseManager {

    public DatabaseManager(Path ignored) {}

    public Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    public Path getDatabasePath() {
        return Path.of("raez.db");
    }
}
