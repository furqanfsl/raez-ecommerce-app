package com.reaz.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Singleton SQLite connection.
 * Creates ALL tables on startup and seeds realistic data.
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    private static final String DB_PATH =
        System.getProperty("user.home") + "/raez/raez.db";

    private DBConnection() {
        connect();
        createTables();
        seedDefaultUsers();
        seedSupportingData();
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
        return connection;
    }

    private void connect() {
        try {
            new java.io.File(System.getProperty("user.home") + "/raez").mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            System.out.println("SQLite connected: " + DB_PATH);
        } catch (Exception e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
    }

    // ── Table Creation ────────────────────────────────────────────────────

    private void createTables() {
        String[] tables = {
            // Categories
            """
            CREATE TABLE IF NOT EXISTS category (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT NOT NULL UNIQUE,
                description TEXT,
                parent_id   INTEGER,
                status      TEXT DEFAULT 'ACTIVE',
                FOREIGN KEY (parent_id) REFERENCES category(id)
            )
            """,
            // Products
            """
            CREATE TABLE IF NOT EXISTS product (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                sku         TEXT NOT NULL UNIQUE,
                name        TEXT NOT NULL,
                description TEXT,
                price       REAL NOT NULL CHECK(price > 0),
                status      TEXT DEFAULT 'ACTIVE',
                created_at  TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at  TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """,
            // Product ↔ Category junction
            """
            CREATE TABLE IF NOT EXISTS product_categories (
                product_id  INTEGER,
                category_id INTEGER,
                PRIMARY KEY (product_id, category_id),
                FOREIGN KEY (product_id)  REFERENCES product(id)  ON DELETE CASCADE,
                FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
            )
            """,
            // Product images
            """
            CREATE TABLE IF NOT EXISTS product_image (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id  INTEGER NOT NULL,
                image_path  TEXT NOT NULL,
                is_primary  INTEGER DEFAULT 0,
                uploaded_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
            )
            """,
            // Product validation log
            """
            CREATE TABLE IF NOT EXISTS product_validation (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id      INTEGER NOT NULL,
                validated_by    TEXT,
                validation_date TEXT DEFAULT CURRENT_TIMESTAMP,
                status          TEXT,
                message         TEXT,
                FOREIGN KEY (product_id) REFERENCES product(id)
            )
            """,
            // Users
            """
            CREATE TABLE IF NOT EXISTS fuser (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT NOT NULL,
                email         TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role          TEXT NOT NULL,
                status        TEXT DEFAULT 'ACTIVE',
                created_at    TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """,
            // Admin users
            """
            CREATE TABLE IF NOT EXISTS admin_user (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE,
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Login credentials
            """
            CREATE TABLE IF NOT EXISTS login_credentials (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER UNIQUE,
                username      TEXT UNIQUE,
                password_hash TEXT,
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Customer registration
            """
            CREATE TABLE IF NOT EXISTS customer_registration (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE,
                phone   TEXT,
                address TEXT,
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Customer preferences
            """
            CREATE TABLE IF NOT EXISTS customer_preferences (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id              INTEGER UNIQUE,
                preferred_categories TEXT,
                delivery_notes       TEXT,
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Customer updates log
            """
            CREATE TABLE IF NOT EXISTS customer_update (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER,
                updated_field TEXT,
                old_value     TEXT,
                new_value     TEXT,
                update_date   TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Orders
            """
            CREATE TABLE IF NOT EXISTS orders (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id      INTEGER NOT NULL,
                order_date   TEXT DEFAULT CURRENT_TIMESTAMP,
                status       TEXT DEFAULT 'PROCESSING',
                total_amount REAL CHECK(total_amount >= 0),
                FOREIGN KEY (user_id) REFERENCES fuser(id)
            )
            """,
            // Order items
            """
            CREATE TABLE IF NOT EXISTS order_item (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id   INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity   INTEGER NOT NULL CHECK(quantity > 0),
                unit_price REAL NOT NULL CHECK(unit_price > 0),
                line_total REAL NOT NULL CHECK(line_total >= 0),
                FOREIGN KEY (order_id)   REFERENCES orders(id),
                FOREIGN KEY (product_id) REFERENCES product(id)
            )
            """,
            // Payment
            """
            CREATE TABLE IF NOT EXISTS payment (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id       INTEGER UNIQUE,
                amount         REAL NOT NULL CHECK(amount > 0),
                payment_method TEXT,
                payment_date   TEXT DEFAULT CURRENT_TIMESTAMP,
                status         TEXT,
                FOREIGN KEY (order_id) REFERENCES orders(id)
            )
            """,
            // Refunds
            """
            CREATE TABLE IF NOT EXISTS refund (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                payment_id  INTEGER,
                product_id  INTEGER,
                amount      REAL CHECK(amount >= 0),
                reason      TEXT,
                status      TEXT,
                refund_date TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (payment_id) REFERENCES payment(id),
                FOREIGN KEY (product_id) REFERENCES product(id)
            )
            """,
            // Warehouse
            """
            CREATE TABLE IF NOT EXISTS warehouse (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name     TEXT NOT NULL,
                location TEXT,
                capacity INTEGER,
                status   TEXT DEFAULT 'ACTIVE'
            )
            """,
            // Inventory records
            """
            CREATE TABLE IF NOT EXISTS inventory_record (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id   INTEGER NOT NULL,
                warehouse_id INTEGER NOT NULL,
                quantity     INTEGER NOT NULL CHECK(quantity >= 0),
                min_stock    INTEGER DEFAULT 0,
                last_updated TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id)   REFERENCES product(id),
                FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
                UNIQUE(product_id, warehouse_id)
            )
            """,
            // Stock movements
            """
            CREATE TABLE IF NOT EXISTS stock_movement (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                inventory_id  INTEGER NOT NULL,
                change_amount INTEGER NOT NULL,
                movement_type TEXT,
                movement_date TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (inventory_id) REFERENCES inventory_record(id)
            )
            """,
            // Drivers
            """
            CREATE TABLE IF NOT EXISTS driver (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                name           TEXT NOT NULL,
                license_number TEXT UNIQUE,
                phone          TEXT,
                status         TEXT DEFAULT 'ACTIVE'
            )
            """,
            // Delivery
            """
            CREATE TABLE IF NOT EXISTS delivery (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id        INTEGER UNIQUE,
                driver_id       INTEGER,
                delivery_status TEXT,
                delivery_date   TEXT,
                FOREIGN KEY (order_id)  REFERENCES orders(id),
                FOREIGN KEY (driver_id) REFERENCES driver(id)
            )
            """,
            // Delivery log
            """
            CREATE TABLE IF NOT EXISTS delivery_log (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                delivery_id INTEGER,
                status      TEXT,
                change_date TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (delivery_id) REFERENCES delivery(id)
            )
            """,
            // Reviews
            """
            CREATE TABLE IF NOT EXISTS review (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id  INTEGER NOT NULL,
                user_id     INTEGER NOT NULL,
                rating      INTEGER CHECK(rating BETWEEN 1 AND 5),
                comment     TEXT,
                review_date TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES product(id),
                FOREIGN KEY (user_id)    REFERENCES fuser(id),
                UNIQUE(product_id, user_id)
            )
            """,
            // Alerts
            """
            CREATE TABLE IF NOT EXISTS alert (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER,
                order_id   INTEGER,
                severity   TEXT,
                message    TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                resolved   INTEGER DEFAULT 0,
                FOREIGN KEY (product_id) REFERENCES product(id),
                FOREIGN KEY (order_id)   REFERENCES orders(id)
            )
            """,
            // Financial anomalies
            """
            CREATE TABLE IF NOT EXISTS financial_anomalies (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id  INTEGER,
                order_id    INTEGER,
                description TEXT,
                severity    TEXT,
                detected_at TEXT DEFAULT CURRENT_TIMESTAMP,
                resolved    INTEGER DEFAULT 0,
                FOREIGN KEY (product_id) REFERENCES product(id),
                FOREIGN KEY (order_id)   REFERENCES orders(id)
            )
            """,
            // Indexes
            "CREATE INDEX IF NOT EXISTS idx_product_name    ON product(name)",
            "CREATE INDEX IF NOT EXISTS idx_product_sku     ON product(sku)",
            "CREATE INDEX IF NOT EXISTS idx_category_name   ON category(name)",
            "CREATE INDEX IF NOT EXISTS idx_order_status    ON orders(status)",
            "CREATE INDEX IF NOT EXISTS idx_review_product  ON review(product_id)",
            "CREATE INDEX IF NOT EXISTS idx_stock_movement  ON stock_movement(inventory_id)",
            "CREATE INDEX IF NOT EXISTS idx_delivery_order  ON delivery(order_id)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            System.out.println("All tables created/verified.");
        } catch (SQLException e) {
            System.err.println("createTables failed: " + e.getMessage());
        }
    }

    // ── Seed Default Users ────────────────────────────────────────────────

    /** Seeds default users on first run */
    public void seedDefaultUsers() {
        try {
            ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM fuser");
            if (rs.getInt(1) > 0) return;
        } catch (SQLException e) { return; }

        String hash_admin    = hashPassword("admin123");
        String hash_customer = hashPassword("customer123");

        String[][] users = {
            {"Admin",         "admin@raez.com",          hash_admin,    "ADMIN"},
            {"Alice Johnson", "alice@example.com",        hash_customer, "CUSTOMER"},
            {"Bob Smith",     "bob@example.com",          hash_customer, "CUSTOMER"},
            {"Carol White",   "carol@example.com",        hash_customer, "CUSTOMER"},
            {"David Brown",   "david@example.com",        hash_customer, "CUSTOMER"},
            {"Emma Davis",    "emma@example.com",         hash_customer, "CUSTOMER"},
        };

        String sql = "INSERT OR IGNORE INTO fuser (name, email, password_hash, role) VALUES (?,?,?,?)";
        String credSql = "INSERT OR IGNORE INTO login_credentials (user_id, username, password_hash) VALUES (?,?,?)";
        String regSql  = "INSERT OR IGNORE INTO customer_registration (user_id, phone, address) VALUES (?,?,?)";
        String prefSql = "INSERT OR IGNORE INTO customer_preferences (user_id, preferred_categories, delivery_notes) VALUES (?,?,?)";

        String[][] customerDetails = {
            {"+44 7700 900001", "123 High Street, London, W1A 1AA",   "Home Assistants,Companions", "Leave at door"},
            {"+44 7700 900002", "45 Park Lane, Manchester, M1 2AB",   "Security Bots,Industrial",   "Ring doorbell"},
            {"+44 7700 900003", "78 Queen Road, Birmingham, B1 3CD",  "Educational",                "Leave with neighbour"},
            {"+44 7700 900004", "12 King Street, Leeds, LS1 4EF",     "Companions,Home Assistants", "Call before delivery"},
            {"+44 7700 900005", "99 Victoria Ave, Bristol, BS1 5GH",  "Industrial",                 "No specific instructions"},
        };

        try (PreparedStatement ps   = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement cred = connection.prepareStatement(credSql);
             PreparedStatement reg  = connection.prepareStatement(regSql);
             PreparedStatement pref = connection.prepareStatement(prefSql)) {

            int customerIdx = 0;
            for (String[] u : users) {
                ps.setString(1, u[0]);
                ps.setString(2, u[1]);
                ps.setString(3, u[2]);
                ps.setString(4, u[3]);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    int uid = keys.getInt(1);

                    // Login credentials for all users
                    cred.setInt(1, uid);
                    cred.setString(2, u[1]);
                    cred.setString(3, u[2]);
                    cred.executeUpdate();

                    // Customer-specific data
                    if ("CUSTOMER".equals(u[3]) && customerIdx < customerDetails.length) {
                        reg.setInt(1, uid);
                        reg.setString(2, customerDetails[customerIdx][0]);
                        reg.setString(3, customerDetails[customerIdx][1]);
                        reg.executeUpdate();

                        pref.setInt(1, uid);
                        pref.setString(2, customerDetails[customerIdx][2]);
                        pref.setString(3, customerDetails[customerIdx][3]);
                        pref.executeUpdate();
                        customerIdx++;
                    }

                    // Admin user record
                    if ("ADMIN".equals(u[3])) {
                        connection.createStatement().execute(
                            "INSERT OR IGNORE INTO admin_user (user_id) VALUES (" + uid + ")");
                    }
                }
            }
            System.out.println("Default users seeded.");
        } catch (SQLException e) {
            System.err.println("seedDefaultUsers failed: " + e.getMessage());
        }
    }

    // ── Seed Supporting Data ──────────────────────────────────────────────

    /**
     * Seeds drivers, orders, payments, deliveries, reviews, alerts,
     * and stock movements after products exist.
     * Only runs once (checks if data already exists).
     */
    public void seedSupportingData() {
        try {
            ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM driver");
            if (rs.getInt(1) > 0) return;
        } catch (SQLException e) { return; }

        seedDrivers();
        seedOrdersAndPayments();
        seedDeliveries();
        seedReviews();
        seedAlerts();
        seedStockMovements();
        System.out.println("Supporting data seeded.");
    }

    private void seedDrivers() {
        String[][] drivers = {
            {"James Wilson",  "DL-UK-001", "+44 7700 100001"},
            {"Sarah Connor",  "DL-UK-002", "+44 7700 100002"},
            {"Mike Taylor",   "DL-UK-003", "+44 7700 100003"},
            {"Lucy Evans",    "DL-UK-004", "+44 7700 100004"},
            {"Tom Harrison",  "DL-UK-005", "+44 7700 100005"},
        };
        try {
            String sql = "INSERT OR IGNORE INTO driver (name, license_number, phone) VALUES (?,?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            for (String[] d : drivers) {
                ps.setString(1, d[0]); ps.setString(2, d[1]); ps.setString(3, d[2]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("seedDrivers: " + e.getMessage()); }
    }

    private void seedOrdersAndPayments() {
        try {
            // Get user IDs (customers only)
            ResultSet rs = connection.createStatement().executeQuery(
                "SELECT id FROM fuser WHERE role='CUSTOMER' LIMIT 5");
            java.util.List<Integer> userIds = new java.util.ArrayList<>();
            while (rs.next()) userIds.add(rs.getInt(1));
            if (userIds.isEmpty()) return;

            // Get product IDs and prices
            ResultSet prs = connection.createStatement().executeQuery(
                "SELECT id, price FROM product WHERE status='ACTIVE' LIMIT 10");
            java.util.List<int[]> products = new java.util.ArrayList<>();
            while (prs.next()) products.add(new int[]{prs.getInt(1), (int)(prs.getDouble(2) * 100)});
            if (products.isEmpty()) return;

            String[] statuses = {"PROCESSING", "SHIPPED", "DELIVERED", "DELIVERED", "DELIVERED"};
            String[] methods  = {"CARD", "PAYPAL", "CARD", "BANK_TRANSFER", "CARD"};
            String[] dates    = {
                "2026-01-15 10:30:00", "2026-01-22 14:15:00", "2026-02-05 09:00:00",
                "2026-02-14 16:45:00", "2026-03-01 11:20:00"
            };

            for (int i = 0; i < Math.min(5, userIds.size()); i++) {
                int[] prod = products.get(i % products.size());
                int qty   = (i % 3) + 1;
                double unitPrice = prod[1] / 100.0;
                double total     = unitPrice * qty;

                // Insert order
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders (user_id, order_date, status, total_amount) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, userIds.get(i));
                ps.setString(2, dates[i]);
                ps.setString(3, statuses[i]);
                ps.setDouble(4, total);
                ps.executeUpdate();
                int orderId = ps.getGeneratedKeys().getInt(1);

                // Insert order item
                PreparedStatement oi = connection.prepareStatement(
                    "INSERT INTO order_item (order_id, product_id, quantity, unit_price, line_total) VALUES (?,?,?,?,?)");
                oi.setInt(1, orderId); oi.setInt(2, prod[0]);
                oi.setInt(3, qty);     oi.setDouble(4, unitPrice); oi.setDouble(5, total);
                oi.executeUpdate();

                // Insert payment
                PreparedStatement pay = connection.prepareStatement(
                    "INSERT INTO payment (order_id, amount, payment_method, payment_date, status) VALUES (?,?,?,?,?)");
                pay.setInt(1, orderId);  pay.setDouble(2, total);
                pay.setString(3, methods[i]); pay.setString(4, dates[i]);
                pay.setString(5, "COMPLETED");
                pay.executeUpdate();
            }

            // Add one refund for first order
            ResultSet payRs = connection.createStatement()
                .executeQuery("SELECT id FROM payment LIMIT 1");
            if (payRs.next()) {
                int[] prod = products.get(0);
                PreparedStatement ref = connection.prepareStatement(
                    "INSERT INTO refund (payment_id, product_id, amount, reason, status) VALUES (?,?,?,?,?)");
                ref.setInt(1, payRs.getInt(1)); ref.setInt(2, prod[0]);
                ref.setDouble(3, prod[1] / 100.0);
                ref.setString(4, "Product damaged on arrival");
                ref.setString(5, "APPROVED");
                ref.executeUpdate();
            }

        } catch (SQLException e) { System.err.println("seedOrders: " + e.getMessage()); }
    }

    private void seedDeliveries() {
        try {
            ResultSet rs = connection.createStatement()
                .executeQuery("SELECT id, status FROM orders");
            ResultSet drs = connection.createStatement()
                .executeQuery("SELECT id FROM driver");

            java.util.List<Integer> driverIds = new java.util.ArrayList<>();
            while (drs.next()) driverIds.add(drs.getInt(1));
            if (driverIds.isEmpty()) return;

            int dIdx = 0;
            while (rs.next()) {
                int orderId = rs.getInt(1);
                String status = rs.getString(2);
                String delivStatus = "PROCESSING".equals(status) ? "PENDING"
                    : "SHIPPED".equals(status) ? "IN_TRANSIT" : "DELIVERED";
                String delivDate = "DELIVERED".equals(delivStatus) ? "2026-03-10" : null;

                PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO delivery (order_id, driver_id, delivery_status, delivery_date) VALUES (?,?,?,?)");
                ps.setInt(1, orderId);
                ps.setInt(2, driverIds.get(dIdx % driverIds.size()));
                ps.setString(3, delivStatus);
                ps.setString(4, delivDate);
                ps.executeUpdate();
                int delivId = ps.getGeneratedKeys().getInt(1);

                // Delivery log entry
                PreparedStatement log = connection.prepareStatement(
                    "INSERT INTO delivery_log (delivery_id, status) VALUES (?,?)");
                log.setInt(1, delivId); log.setString(2, delivStatus);
                log.executeUpdate();

                dIdx++;
            }
        } catch (SQLException e) { System.err.println("seedDeliveries: " + e.getMessage()); }
    }

    private void seedReviews() {
        try {
            ResultSet prs = connection.createStatement()
                .executeQuery("SELECT id FROM product WHERE status='ACTIVE' LIMIT 5");
            ResultSet urs = connection.createStatement()
                .executeQuery("SELECT id FROM fuser WHERE role='CUSTOMER' LIMIT 5");

            java.util.List<Integer> pIds = new java.util.ArrayList<>();
            java.util.List<Integer> uIds = new java.util.ArrayList<>();
            while (prs.next()) pIds.add(prs.getInt(1));
            while (urs.next()) uIds.add(urs.getInt(1));
            if (pIds.isEmpty() || uIds.isEmpty()) return;

            int[][] reviews = {
                {5, 0, 0}, {4, 1, 1}, {5, 2, 2}, {3, 3, 3}, {4, 4, 4}
            };
            String[] comments = {
                "Absolutely love this robot! Works perfectly and easy to set up.",
                "Great product overall, delivery was fast and packaging was excellent.",
                "Impressive build quality. My kids are obsessed with it!",
                "Good product but took a while to arrive. Works as described.",
                "Really happy with this purchase. The AI features are amazing."
            };

            String sql = "INSERT OR IGNORE INTO review (product_id, user_id, rating, comment) VALUES (?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int[] r : reviews) {
                if (r[1] >= pIds.size() || r[2] >= uIds.size()) continue;
                ps.setInt(1, pIds.get(r[1]));
                ps.setInt(2, uIds.get(r[2]));
                ps.setInt(3, r[0]);
                ps.setString(4, comments[r[0] - 1]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("seedReviews: " + e.getMessage()); }
    }

    private void seedAlerts() {
        try {
            ResultSet prs = connection.createStatement()
                .executeQuery("SELECT id FROM product LIMIT 3");
            java.util.List<Integer> pIds = new java.util.ArrayList<>();
            while (prs.next()) pIds.add(prs.getInt(1));
            if (pIds.isEmpty()) return;

            String[][] alerts = {
                {"LOW_STOCK",  "WARNING", "Stock level for product is below minimum threshold"},
                {"LOW_STOCK",  "WARNING", "Stock critically low — only 2 units remaining"},
                {"PRICE_DROP", "INFO",    "Product price dropped by more than 20% this week"},
            };

            String sql = "INSERT INTO alert (product_id, severity, message) VALUES (?,?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int i = 0; i < Math.min(alerts.length, pIds.size()); i++) {
                ps.setInt(1, pIds.get(i));
                ps.setString(2, alerts[i][1]);
                ps.setString(3, alerts[i][2]);
                ps.executeUpdate();
            }

            // One financial anomaly
            if (!pIds.isEmpty()) {
                PreparedStatement fa = connection.prepareStatement(
                    "INSERT INTO financial_anomalies (product_id, description, severity) VALUES (?,?,?)");
                fa.setInt(1, pIds.get(0));
                fa.setString(2, "Unusual price fluctuation detected — price changed 3 times in 24 hours");
                fa.setString(3, "MEDIUM");
                fa.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("seedAlerts: " + e.getMessage()); }
    }

    private void seedStockMovements() {
        try {
            ResultSet irs = connection.createStatement()
                .executeQuery("SELECT id FROM inventory_record LIMIT 5");
            java.util.List<Integer> invIds = new java.util.ArrayList<>();
            while (irs.next()) invIds.add(irs.getInt(1));
            if (invIds.isEmpty()) return;

            String[][] movements = {
                {"+50",  "STOCK_IN",   "Initial stock loaded"},
                {"-5",   "SALE",       "Order #1 fulfilled"},
                {"-3",   "SALE",       "Order #2 fulfilled"},
                {"+20",  "RESTOCK",    "Supplier delivery received"},
                {"-1",   "DAMAGED",    "Unit damaged in warehouse"},
            };

            String sql = "INSERT INTO stock_movement (inventory_id, change_amount, movement_type) VALUES (?,?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int i = 0; i < Math.min(movements.length, invIds.size()); i++) {
                ps.setInt(1, invIds.get(i));
                ps.setInt(2, Integer.parseInt(movements[i][0]));
                ps.setString(3, movements[i][1]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("seedStockMovements: " + e.getMessage()); }
    }

    // ── Password hashing ──────────────────────────────────────────────────

    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md =
                java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return password; }
    }
}