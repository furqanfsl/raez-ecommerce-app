package com.raez.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Singleton SQLite connection.
 * Applies the schema with CREATE IF NOT EXISTS on every startup and seeds
 * only when the users table is empty, so data persists across restarts.
 *   /raez_unified_schema.sql  — table definitions
 *   /raez_seed_data.sql       — seed rows
 * Override DB path with {@code -Draez.db.path=C:/path/to/raez.db}.
 * Force a clean rebuild with {@code -Draez.db.reset=true}.
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

    // SHA-256("raez123") — standardised demo password for all customer accounts
    private static final String RAEZ123_HASH =
        "88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee";

    private DBConnection() {
        if (Boolean.getBoolean("raez.db.reset")) {
            System.out.println("raez.db.reset=true → wiping DB file before boot.");
            deleteDatabaseFiles();
        }
        connect();
        executeSqlFile("/raez_unified_schema.sql");
        migrateProductsCollectionColumn();
        if (isFirstBoot()) {
            System.out.println("Empty DB detected — applying seed data.");
            executeSqlFile("/raez_seed_data.sql");
        } else {
            System.out.println("DB already populated — skipping seed.");
            migrateCustomerPasswords();
            migrateAdminPasswords();
        }
        migrateCollectionProducts();
    }

    /**
     * Phase 4 migration: products.collection column was added after the first
     * releases shipped. On pre-existing databases this silently ALTERs the
     * table; the duplicate-column error on newer DBs is ignored.
     */
    private void migrateProductsCollectionColumn() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("ALTER TABLE products ADD COLUMN collection TEXT");
            System.out.println("Migrated products schema: added 'collection' column.");
        } catch (SQLException e) {
            // "duplicate column name" is the expected case on an already-migrated DB.
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("duplicate column") && !msg.contains("already exists")) {
                System.err.println("migrateProductsCollectionColumn warning: " + e.getMessage());
            }
        }
    }

    /**
     * One-time migration: standardises customer demo account passwords to raez123.
     * Only updates rows that still carry one of the three legacy hash values
     * that were seeded before the password was standardised.
     */
    private void migrateCustomerPasswords() {
        String[] legacyHashes = {
            "4e40e8ffe0ee32fa53e139147ed559229a5930f89c2204706fc174beb36210b3", // alice
            "56318228b3a39a2af341c080cc2d8b1d7e088ed24bd28d6cc9b34a8711253434", // omar
            "926b4b8a00cfab44b758450fa6bf188d4bf8541c2fd6b3d9b93d152d43a99f64", // sara
            "3688058a6965c4c8e143d7002afb557fe910657ad819714abb0356c7551c84b7", // maya
            "4eb84dcc7275bc750ea32fbfe061fc0477d7d332ed1071c1e06911ddec3a6556"  // zaid
        };
        String sql =
            "UPDATE users SET passwordHash = ?, updatedAt = CURRENT_TIMESTAMP " +
            "WHERE passwordHash = ?";
        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String oldHash : legacyHashes) {
                ps.setString(1, RAEZ123_HASH);
                ps.setString(2, oldHash);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Migrated customer password hash: " + oldHash.substring(0, 8) + "…");
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("migrateCustomerPasswords warning: " + e.getMessage());
        }
    }

    /**
     * Migrates integration admin accounts to the standardised raez123 password hash.
     * Only updates rows that still carry the old non-raez123 hashes so repeated runs are safe.
     */
    private void migrateAdminPasswords() {
        // Emails whose hashes may be stale from an older seed
        String[] adminEmails = {
            "adminProduct@raez.org.uk", "adminCustomer@raez.org.uk",
            "adminWarehouse@raez.org.uk", "adminDelivery@raez.org.uk",
            "adminFinance@raez.org.uk", "adminReviews@raez.org.uk",
            "products@raez.org.uk", "delivery@raez.org.uk",
            "orders@raez.org.uk", "reviews@raez.org.uk",
            "finance@raez.org.uk", "warehouse@raez.org.uk",
            "admin@raez.org.uk.wh"
        };
        // SHA-256("admin123") for the super-admin
        String admin123Hash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        String sql = "UPDATE users SET passwordHash = ?, updatedAt = CURRENT_TIMESTAMP " +
                     "WHERE email = ? AND passwordHash <> ?";
        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String email : adminEmails) {
                String targetHash = "admin@raez.org.uk".equals(email) ? admin123Hash : RAEZ123_HASH;
                ps.setString(1, targetHash);
                ps.setString(2, email);
                ps.setString(3, targetHash);
                int rows = ps.executeUpdate();
                if (rows > 0) System.out.println("Migrated admin password for: " + email);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("migrateAdminPasswords warning: " + e.getMessage());
        }
    }

    /**
     * Idempotent migration: inserts Phase-4 collection products (IDs 300-343)
     * plus their category links, images, and inventory using INSERT OR IGNORE.
     * Runs on every startup — safe on both fresh and existing databases.
     */
    private void migrateCollectionProducts() {
        try {
            executeSqlFile("/raez_migration_v2_collections.sql");
            System.out.println("migrateCollectionProducts: collection data ensured.");
        } catch (Exception e) {
            System.err.println("migrateCollectionProducts warning: " + e.getMessage());
        }
    }

    /** @return true if users table has no rows (first boot, nothing to preserve). */
    private boolean isFirstBoot() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            // users table missing → schema ran but empty → seed
            return true;
        }
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
