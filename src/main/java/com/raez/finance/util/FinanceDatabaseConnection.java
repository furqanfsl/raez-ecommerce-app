package com.raez.finance.util;

import com.raez.db.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Returns a fresh JDBC connection per call (unified raez.db).
 * <p>
 * Finance DAOs use try-with-resources, which closes the connection on exit.
 * Returning the shared {@link DBConnection#getInstance()} singleton would
 * close that singleton mid-flight and break every other in-progress query
 * (observed as "stmt pointer is closed" / "database connection closed").
 * Per {@code DBConnection#openNew()} javadoc, off-FX-thread code MUST get
 * its own Connection — that's exactly what every finance DAO does.
 */
public final class FinanceDatabaseConnection {

    private FinanceDatabaseConnection() {}

    public static void resetForTests() {}

    public static Connection getConnection() throws SQLException {
        // Ensure singleton init (schema + seed) has run, then hand out a fresh connection.
        DBConnection.getInstance();
        return DBConnection.openNew();
    }
}
