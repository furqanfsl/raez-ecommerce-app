package com.raez.finance.util;

import com.raez.db.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/** Delegates to the master DBConnection singleton (unified raez.db). */
public final class FinanceDatabaseConnection {

    private FinanceDatabaseConnection() {}

    public static void resetForTests() {}

    public static Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }
}
