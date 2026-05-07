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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);


    private static DBConnection instance;
    private Connection connection;
    private static final String DB_PATH = resolveDbPath();

    private static String resolveDbPath() {
        String override = System.getProperty("raez.db.path");
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize().toString();
        }
        // Prefer the working directory when it's writable (dev mode: repo root).
        // Fall back to ~/.raez/raez.db when it isn't (installed app launched from
        // Program Files, where SQLite would otherwise hit "Access is denied").
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isWritable(cwd)) {
            return cwd.resolve("raez.db").toString();
        }
        Path userHome = Paths.get(System.getProperty("user.home"), ".raez", "raez.db")
            .toAbsolutePath()
            .normalize();
        log.info("{}", "Working dir not writable; using DB at " + userHome);
        return userHome.toString();
    }

    // SHA-256("raez123") — standardised demo password for all customer accounts
    private static final String RAEZ123_HASH =
        "88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee";

    private DBConnection() {
        if (Boolean.getBoolean("raez.db.reset")) {
            log.info("{}", "raez.db.reset=true → wiping DB file before boot.");
            deleteDatabaseFiles();
        }
        connect();
        executeSqlFile("/raez_unified_schema.sql");
        migrateProductsCollectionColumn();
        migrateProductsCollectionIdColumn();
        migrateProductsImageUrlColumns();
        if (isFirstBoot()) {
            log.info("{}", "Empty DB detected — applying seed data.");
            executeSqlFile("/raez_seed_data.sql");
        } else {
            log.info("{}", "DB already populated — skipping seed.");
            migrateCustomerPasswords();
            migrateAdminPasswords();
        }
        migrateCollectionProducts();
        migrateRoboticsStoreCatalog();
        migrateProductImageBackfill();
        migrateProductImageDefaults();
    }

    /**
     * Backfills the real Cloudinary imageUrls captured from the dev DB for every
     * SKU that was uploaded through the admin UI. Idempotent — every UPDATE is
     * gated by {@code WHERE imageUrl IS NULL OR imageUrl = ''}, so a dev DB with
     * its own uploads is never overwritten.
     */
    private void migrateProductImageBackfill() {
        try {
            executeSqlFile("/raez_migration_v4_image_backfill.sql");
        } catch (RuntimeException e) {
            log.error("{}", "migrateProductImageBackfill warning: " + e.getMessage());
        }
    }

    /**
     * Idempotent: assigns a public placeholder image URL to any product whose
     * imageUrl is still NULL. Lets a freshly-installed app (e.g. the jpackage
     * .exe spinning up `~/.raez/raez.db`) render a populated catalog without
     * the recruiter needing Cloudinary creds. Skipped per-row when the user
     * has already uploaded a real image, so dev DBs aren't overwritten.
     */
    private void migrateProductImageDefaults() {
        // Series flagships + a per-category fallback. Public Unsplash CDN URLs.
        String robotMain     = "https://images.unsplash.com/photo-1535378917042-10a22c95931a?w=600&q=80";
        String robotMini     = "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=600&q=80";
        String robotAccessor = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80";
        String robotService  = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600&q=80";

        java.util.Map<String, String> bySku = java.util.Map.ofEntries(
            java.util.Map.entry("HTR-001", "https://images.unsplash.com/photo-1589254065878-42c9da997008?w=600&q=80"),
            java.util.Map.entry("HTR-013", "https://images.unsplash.com/photo-1546776230-bb86256870ce?w=600&q=80"),
            java.util.Map.entry("HTR-025", "https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=600&q=80"),
            java.util.Map.entry("HTR-037", "https://images.unsplash.com/photo-1531746790731-6c087fecd65a?w=600&q=80"),
            java.util.Map.entry("HTR-049", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600&q=80"),
            java.util.Map.entry("HTR-050", "https://images.unsplash.com/photo-1581090700227-1e37b190418e?w=600&q=80"),
            java.util.Map.entry("HTR-051", "https://images.unsplash.com/photo-1605379399642-870262d3d051?w=600&q=80")
        );

        try {
            // Per-SKU overrides for series flagships.
            try (java.sql.PreparedStatement ps = connection.prepareStatement(
                    "UPDATE products SET imageUrl = ? WHERE sku = ? AND (imageUrl IS NULL OR imageUrl = '')")) {
                for (var entry : bySku.entrySet()) {
                    ps.setString(1, entry.getValue());
                    ps.setString(2, entry.getKey());
                    ps.executeUpdate();
                }
            }

            // Per-category fallbacks for any HTR product still without an image.
            String catUpdate =
                "UPDATE products SET imageUrl = ? " +
                "WHERE (imageUrl IS NULL OR imageUrl = '') " +
                "  AND sku LIKE 'HTR-%' " +
                "  AND categoryID IN (SELECT categoryID FROM categories WHERE categoryName = ?)";
            try (java.sql.PreparedStatement ps = connection.prepareStatement(catUpdate)) {
                for (Object[] pair : new Object[][]{
                        {robotMain, "Main Robot"},
                        {robotMini, "Mini Robot"},
                        {robotAccessor, "Accessory"},
                        {robotService, "Service"}}) {
                    ps.setString(1, (String) pair[0]);
                    ps.setString(2, (String) pair[1]);
                    ps.executeUpdate();
                }
            }

            // Catch-all for any remaining product with no image (non-HTR).
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                    "UPDATE products SET imageUrl = '" + robotMain + "' " +
                    "WHERE imageUrl IS NULL OR imageUrl = ''");
            }

            log.info("{}", "migrateProductImageDefaults: ensured placeholder imageUrls.");
        } catch (SQLException e) {
            log.error("{}", "migrateProductImageDefaults warning: " + e.getMessage());
        }
    }

    /**
     * Phase 4 migration: products.collection column was added after the first
     * releases shipped. On pre-existing databases this silently ALTERs the
     * table; the duplicate-column error on newer DBs is ignored.
     */
    private void migrateProductsCollectionColumn() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("ALTER TABLE products ADD COLUMN collection TEXT");
            log.info("{}", "Migrated products schema: added 'collection' column.");
        } catch (SQLException e) {
            // "duplicate column name" is the expected case on an already-migrated DB.
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("duplicate column") && !msg.contains("already exists")) {
                log.error("{}", "migrateProductsCollectionColumn warning: " + e.getMessage());
            }
        }
    }

    /**
     * Phase 5 migration: products.collectionID column added for robotics store.
     * Silently ignored when the column already exists.
     */
    private void migrateProductsCollectionIdColumn() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("ALTER TABLE products ADD COLUMN collectionID INTEGER");
            log.info("{}", "Migrated products schema: added 'collectionID' column.");
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("duplicate column") && !msg.contains("already exists")) {
                log.error("{}", "migrateProductsCollectionIdColumn warning: " + e.getMessage());
            }
        }
    }

    /**
     * Day-5 migration: adds imageUrl + imagePublicId for the Cloudinary storage layer.
     */
    private void migrateProductsImageUrlColumns() {
        for (String col : new String[]{"imageUrl", "imagePublicId"}) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE products ADD COLUMN " + col + " TEXT");
                log.info("{}", "Migrated products schema: added '" + col + "' column.");
            } catch (SQLException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (!msg.contains("duplicate column") && !msg.contains("already exists")) {
                    log.error("{}", "migrateProductsImageUrlColumns warning: " + e.getMessage());
                }
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
                    log.info("{}", "Migrated customer password hash: " + oldHash.substring(0, 8) + "…");
                }
            }
        } catch (java.sql.SQLException e) {
            log.error("{}", "migrateCustomerPasswords warning: " + e.getMessage());
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
        // Skip rows already migrated to BCrypt ($2a$/$2b$/$2y$) — otherwise this
        // would clobber the post-MigratePasswords hashes back to SHA-256 on every boot.
        String sql = "UPDATE users SET passwordHash = ?, updatedAt = CURRENT_TIMESTAMP " +
                     "WHERE email = ? AND passwordHash <> ? AND passwordHash NOT LIKE '$2%'";
        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String email : adminEmails) {
                String targetHash = "admin@raez.org.uk".equals(email) ? admin123Hash : RAEZ123_HASH;
                ps.setString(1, targetHash);
                ps.setString(2, email);
                ps.setString(3, targetHash);
                int rows = ps.executeUpdate();
                if (rows > 0) log.info("{}", "Migrated admin password for: " + email);
            }
        } catch (java.sql.SQLException e) {
            log.error("{}", "migrateAdminPasswords warning: " + e.getMessage());
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
            log.info("{}", "migrateCollectionProducts: collection data ensured.");
        } catch (Exception e) {
            log.error("{}", "migrateCollectionProducts warning: " + e.getMessage());
        }
    }

    /**
     * Robotics storefront migration:
     * - creates explicit collection table
     * - surgically clears legacy/placeholder robotics catalogue rows
     * - seeds the high-tech robotics products without image URLs
     * Skipped when HTR-sku products already exist so user data (images, edits) is preserved.
     */
    private void migrateRoboticsStoreCatalog() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM products WHERE sku LIKE 'HTR-%'")) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.info("{}", "migrateRoboticsStoreCatalog: HTR products already exist, skipping re-seed.");
                return;
            }
        } catch (SQLException e) {
            // products table not yet ready — fall through to run migration
        }
        try {
            executeSqlFile("/raez_migration_v3_robotics_store.sql");
            log.info("{}", "migrateRoboticsStoreCatalog: robotics catalogue seeded.");
        } catch (Exception e) {
            log.error("{}", "migrateRoboticsStoreCatalog warning: " + e.getMessage());
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

    /**
     * Returns the long-lived JDBC connection used by the JavaFX UI thread.
     * Reconnects automatically if the underlying Connection has been closed.
     *
     * Threading: this Connection is shared. SQLite in WAL mode allows concurrent
     * readers across separate Connections, but a single Connection is not
     * thread-safe for concurrent use. Code that runs off the FX Application
     * Thread — e.g. {@code javafx.concurrent.Task} background work added in
     * D6.3 — MUST call {@link #openNew()} to get its own Connection instead of
     * sharing this one.
     *
     * No connection pool (HikariCP / DBCP / etc.) on purpose: this is a
     * single-user JavaFX desktop app on a local SQLite file in WAL mode.
     * Pooling here is theatre — fresh Connections are cheap with file-backed
     * SQLite, and a pool would just be resume-padding that an experienced
     * reviewer would flag.
     */
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

    /**
     * Opens and returns a fresh JDBC connection to the SQLite file. Caller MUST
     * close it (use try-with-resources). Use this from any code that runs off
     * the JavaFX Application Thread (e.g. background Tasks added in D6.3) so
     * concurrent DB access doesn't share a single Connection.
     *
     * See {@link #getConnection()} for the rationale on why there is no
     * connection pool.
     */
    public static Connection openNew() {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + DB_PATH.replace('\\', '/');
            Connection c = DriverManager.getConnection(url);
            c.setAutoCommit(true);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return c;
        } catch (Exception e) {
            throw new IllegalStateException("openNew failed: " + e.getMessage(), e);
        }
    }

    // ── private helpers ────────────────────────────────────────────────────

    private void deleteDatabaseFiles() {
        for (String suffix : new String[]{"", "-wal", "-shm"}) {
            try {
                Files.deleteIfExists(Paths.get(DB_PATH + suffix));
            } catch (Exception e) {
                log.error("{}", "DB cleanup warning (" + suffix + "): " + e.getMessage());
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
            log.info("{}", "SQLite connected: " + DB_PATH);
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
            log.info("{}", "Executed SQL resource: " + classpathResource);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to execute " + classpathResource + ": " + e.getMessage(), e);
        }
    }

    /**
     * Strips single-line SQL comments and splits on semicolons, both string-literal aware
     * so that semicolons or `--` sequences inside `'...'` text don't terminate a statement.
     */
    private void executeSqlScript(String script) throws SQLException {
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int len = script.length();
        for (int i = 0; i < len; i++) {
            char c = script.charAt(i);
            if (inString) {
                current.append(c);
                if (c == '\'') {
                    if (i + 1 < len && script.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
                current.append(c);
                continue;
            }
            if (c == '-' && i + 1 < len && script.charAt(i + 1) == '-') {
                while (i < len && script.charAt(i) != '\n') i++;
                if (i < len) current.append('\n');
                continue;
            }
            if (c == ';') {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.toString().trim().length() > 0) {
            statements.add(current.toString());
        }

        try (Statement stmt = connection.createStatement()) {
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        log.error("{}", "SQL warning (skipped): " + e.getMessage()
                            + " | " + trimmed.substring(0, Math.min(80, trimmed.length())));
                    }
                }
            }
        }
    }

}
