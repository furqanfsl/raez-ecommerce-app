package com.raez.finance.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified DB bootstrapper.
 * Applies only the canonical schema/seed SQL shipped in resources/database.
 */
public class FinanceDatabaseInitialiser {
    private static final Logger log = LoggerFactory.getLogger(FinanceDatabaseInitialiser.class);


    public static void main(String[] args) {
        try {
            initialise();
            log.info("{}", "[FinanceDatabaseInitialiser] Complete.");
        } catch (Exception e) {
            log.error("{}", "[FinanceDatabaseInitialiser] FAILED: " + e.getMessage());
            log.error("Error", e);
            System.exit(1);
        }
    }

    public static void initialise() throws Exception {
        try (Connection conn = FinanceDatabaseConnection.getConnection()) {
            exec(conn, "PRAGMA foreign_keys = ON");
            applySqlResource(conn, "/database/schema.sql");
            if (isInitialisationRequired(conn)) {
                applySqlResource(conn, "/database/seed.sql");
            }
        }
    }

    public static boolean isInitialisationRequired(Connection conn) throws SQLException {
        if (!tableExists(conn, "users") || !tableExists(conn, "roles")
                || !tableExists(conn, "user_roles") || !tableExists(conn, "finance_invoices")) {
            return true;
        }
        return isBelowThreshold(conn, "users", 1) || isBelowThreshold(conn, "roles", 1);
    }

    private static void applySqlResource(Connection conn, String classpathResource) throws Exception {
        try (InputStream is = FinanceDatabaseInitialiser.class.getResourceAsStream(classpathResource)) {
            if (is == null) throw new IllegalStateException(classpathResource + " not found on classpath");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
            }
            for (String raw : sb.toString().split(";")) {
                String sql = stripComments(raw).trim();
                if (!sql.isEmpty()) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sql);
                    }
                }
            }
        }
    }

    public static void applyCompatibilityMigrations(Connection conn) throws SQLException {
        // No-op: compatibility is now provided by the unified schema itself.
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String normalized = tableName.replace("\"", "");
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        )) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isBelowThreshold(Connection conn, String table, int threshold) throws SQLException {
        return queryInt(conn, "SELECT COUNT(*) FROM " + table) < threshold;
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement()) { s.execute(sql); }
    }

    private static int queryInt(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static String stripComments(String block) {
        StringBuilder out = new StringBuilder();
        for (String line : block.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                out.append(line).append("\n");
            }
        }
        return out.toString();
    }
}
