package com.reaz.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Singleton SQLite connection.
 * On every startup the DB file is deleted and rebuilt from:
 *   /raez_unified_schema.sql  — table definitions
 *   /raez_seed_data.sql       — seed rows
 * Override DB path with {@code -Draez.db.path=C:/path/to/raez.db}.
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    private static final String DB_PATH = resolveDbPath();

    private static String resolveDbPath() {
        String override = System.getProperty("raez.db.path");
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize().toString();
        }
        return Paths.get(System.getProperty("user.dir"), "raez.db")
            .toAbsolutePath()
            .normalize()
            .toString();
    }

    private DBConnection() {
        // Always start fresh so schema + seed are the single source of truth
        deleteDatabaseFiles();
        connect();
        executeSqlFile("/raez_unified_schema.sql");
        executeSqlFile("/raez_seed_data.sql");
        System.out.println("Database initialised from SQL resource files.");
    }

    public static DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) connect();
        } catch (SQLException e) {
            connect();
        }
        if (connection == null) {
            throw new IllegalStateException("Database connection is not available.");
        }
        return connection;
    }

    // ── private helpers ────────────────────────────────────────────────────

    private void deleteDatabaseFiles() {
        for (String suffix : new String[]{"", "-wal", "-shm"}) {
            try {
                Files.deleteIfExists(Paths.get(DB_PATH + suffix));
            } catch (Exception e) {
                System.err.println("DB cleanup warning (" + suffix + "): " + e.getMessage());
            }
        }
    }

    private void connect() {
        try {
            Path dbFile = Paths.get(DB_PATH);
            Path parent = dbFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + DB_PATH.replace('\\', '/');
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA journal_mode = WAL");
            }
            System.out.println("SQLite connected: " + DB_PATH);
        } catch (Exception e) {
            connection = null;
            throw new IllegalStateException("DB connection failed: " + e.getMessage(), e);
        }
    }

    private void executeSqlFile(String classpathResource) {
        try (InputStream is = DBConnection.class.getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalStateException("SQL resource not found on classpath: " + classpathResource);
            }
            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }
            executeSqlScript(sql);
            System.out.println("Executed SQL resource: " + classpathResource);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to execute " + classpathResource + ": " + e.getMessage(), e);
        }
    }

    /**
     * Strips single-line SQL comments, splits on semicolons, and executes each statement.
     * Handles the BEGIN TRANSACTION / COMMIT wrapping in the seed file correctly.
     */
    private void executeSqlScript(String script) throws SQLException {
        // Strip single-line comments (-- to end of line)
        String[] lines = script.split("\n");
        StringBuilder stripped = new StringBuilder();
        for (String line : lines) {
            int commentIdx = line.indexOf("--");
            stripped.append(commentIdx >= 0 ? line.substring(0, commentIdx) : line);
            stripped.append('\n');
        }

        String[] statements = stripped.toString().split(";");
        try (Statement stmt = connection.createStatement()) {
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        // Log but continue — e.g. duplicate inserts on reconnect
                        System.err.println("SQL warning (skipped): " + e.getMessage()
                            + " | " + trimmed.substring(0, Math.min(80, trimmed.length())));
                    }
                }
            }
        }
    }

    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md =
                java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
