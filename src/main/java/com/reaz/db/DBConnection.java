package com.reaz.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton SQLite connection.
 * Creates unified schema tables on startup and seeds default data.
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    private static final String DB_PATH =
        System.getProperty("user.home") + "/raez/main_raez.db";

    private DBConnection() {
        connect();
        createTables();
        createIndexes();
        seedAll();
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
            new java.io.File(DB_PATH).delete();
            new java.io.File(System.getProperty("user.home") + "/raez").mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA journal_mode = WAL");
            }
            System.out.println("SQLite connected: " + DB_PATH);
        } catch (Exception e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
    }

    /**
     * Creates all unified-schema tables in dependency order (parents before children).
     * DDL matches 1_unified_schema; {@code warehouse_warehouses} is created before
     * {@code finance_supplier_orders} so foreign keys resolve.
     */
    private void createTables() {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS roles (
                roleID      INTEGER PRIMARY KEY AUTOINCREMENT,
                roleName    TEXT    NOT NULL UNIQUE,
                description TEXT,
                createdAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS users (
                userID       INTEGER PRIMARY KEY AUTOINCREMENT,
                email        TEXT    NOT NULL UNIQUE,
                username     TEXT    NOT NULL UNIQUE,
                passwordHash TEXT    NOT NULL,
                firstName    TEXT,
                lastName     TEXT,
                phone        TEXT,
                isActive     INTEGER NOT NULL DEFAULT 1,
                lastLogin    TEXT,
                createdAt    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updatedAt    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS user_roles (
                userRoleID INTEGER PRIMARY KEY AUTOINCREMENT,
                userID     INTEGER NOT NULL,
                roleID     INTEGER NOT NULL,
                assignedAt TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (userID, roleID),
                FOREIGN KEY (userID) REFERENCES users(userID)   ON DELETE CASCADE,
                FOREIGN KEY (roleID) REFERENCES roles(roleID)   ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS password_reset_tokens (
                tokenID    INTEGER PRIMARY KEY AUTOINCREMENT,
                userID     INTEGER NOT NULL,
                token      TEXT    NOT NULL UNIQUE,
                expiryTime TEXT    NOT NULL,
                isUsed     INTEGER NOT NULL DEFAULT 0,
                createdAt  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS customers (
                customerID    INTEGER PRIMARY KEY AUTOINCREMENT,
                userID        INTEGER,
                name          TEXT    NOT NULL,
                email         TEXT    NOT NULL UNIQUE,
                contactNumber TEXT,
                deliveryAddress TEXT,
                customerType  TEXT    NOT NULL DEFAULT 'Individual',
                idCardImage   TEXT,
                status        TEXT    NOT NULL DEFAULT 'active',
                registeredAt  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS customer_preferences (
                preferenceID          INTEGER PRIMARY KEY AUTOINCREMENT,
                customerID            INTEGER NOT NULL UNIQUE,
                preferredCategories   TEXT,
                notificationSettings  TEXT,
                deliveryInstructions  TEXT,
                FOREIGN KEY (customerID) REFERENCES customers(customerID) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS customer_updates (
                updateID     INTEGER PRIMARY KEY AUTOINCREMENT,
                adminUserID  INTEGER,
                customerID   INTEGER,
                updatedField TEXT,
                oldValue     TEXT,
                newValue     TEXT,
                updateDate   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (adminUserID) REFERENCES users(userID)         ON DELETE SET NULL,
                FOREIGN KEY (customerID)  REFERENCES customers(customerID) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS categories (
                categoryID   INTEGER PRIMARY KEY AUTOINCREMENT,
                categoryName TEXT    NOT NULL UNIQUE,
                description  TEXT,
                parentID     INTEGER,
                isActive     INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (parentID) REFERENCES categories(categoryID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS products (
                productID   INTEGER PRIMARY KEY AUTOINCREMENT,
                sku         TEXT    NOT NULL UNIQUE,
                name        TEXT    NOT NULL,
                description TEXT,
                price       REAL    NOT NULL DEFAULT 0.00,
                unitCost    REAL    NOT NULL DEFAULT 0.00,
                status      TEXT    NOT NULL DEFAULT 'active',
                categoryID  INTEGER,
                createdAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updatedAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (categoryID) REFERENCES categories(categoryID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orders (
                orderID     INTEGER PRIMARY KEY AUTOINCREMENT,
                customerID  INTEGER NOT NULL,
                orderDate   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                totalAmount REAL    NOT NULL DEFAULT 0.00,
                status      TEXT    NOT NULL DEFAULT 'Processing',
                FOREIGN KEY (customerID) REFERENCES customers(customerID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS order_items (
                orderItemID INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID     INTEGER NOT NULL,
                productID   INTEGER NOT NULL,
                quantity    INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
                unitPrice   REAL    NOT NULL DEFAULT 0.00,
                FOREIGN KEY (orderID)   REFERENCES orders(orderID)     ON DELETE CASCADE,
                FOREIGN KEY (productID) REFERENCES products(productID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS payments (
                paymentID      INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID        INTEGER NOT NULL,
                amountPaid     REAL    NOT NULL,
                currency       TEXT    NOT NULL DEFAULT 'GBP',
                paymentMethod  TEXT,
                paymentStatus  TEXT    NOT NULL DEFAULT 'PENDING',
                transactionRef TEXT    UNIQUE,
                paymentDate    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                notes          TEXT,
                createdAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updatedAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (orderID) REFERENCES orders(orderID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_invoices (
                invoiceID     INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID       INTEGER,
                customerID    INTEGER NOT NULL,
                paymentID     INTEGER,
                invoiceNumber TEXT    NOT NULL UNIQUE,
                status        TEXT    NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','PAID','OVERDUE','PARTIAL','CANCELLED')),
                subtotal      REAL    NOT NULL DEFAULT 0.00,
                vatAmount     REAL    NOT NULL DEFAULT 0.00,
                totalAmount   REAL    NOT NULL,
                currency      TEXT    NOT NULL DEFAULT 'GBP',
                issuedAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                dueDate       TEXT,
                paidAt        TEXT,
                notes         TEXT,
                createdAt     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updatedAt     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (orderID)    REFERENCES orders(orderID)         ON DELETE SET NULL,
                FOREIGN KEY (customerID) REFERENCES customers(customerID)   ON DELETE CASCADE,
                FOREIGN KEY (paymentID)  REFERENCES payments(paymentID)     ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_refunds (
                refundID       INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID        INTEGER NOT NULL,
                orderItemID    INTEGER,
                productID      INTEGER,
                refundAmount   REAL    NOT NULL,
                refundDate     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                reason         TEXT,
                status         TEXT    NOT NULL DEFAULT 'REQUESTED'
                               CHECK (status IN ('REQUESTED','APPROVED','REJECTED','PROCESSED')),
                processedByUserID INTEGER,
                approvedAt     TEXT,
                createdAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (orderID)          REFERENCES orders(orderID)         ON DELETE CASCADE,
                FOREIGN KEY (orderItemID)      REFERENCES order_items(orderItemID),
                FOREIGN KEY (productID)        REFERENCES products(productID),
                FOREIGN KEY (processedByUserID) REFERENCES users(userID)          ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_anomalies (
                anomalyID          INTEGER PRIMARY KEY AUTOINCREMENT,
                anomalyType        TEXT,
                description        TEXT,
                severity           TEXT    NOT NULL DEFAULT 'LOW'
                                   CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
                detectionRule      TEXT,
                alertDate          TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                isResolved         INTEGER NOT NULL DEFAULT 0,
                resolvedByUserID   INTEGER,
                affectedCustomerID INTEGER,
                affectedOrderID    INTEGER,
                affectedProductID  INTEGER,
                FOREIGN KEY (resolvedByUserID)   REFERENCES users(userID)          ON DELETE SET NULL,
                FOREIGN KEY (affectedCustomerID) REFERENCES customers(customerID),
                FOREIGN KEY (affectedOrderID)    REFERENCES orders(orderID),
                FOREIGN KEY (affectedProductID)  REFERENCES products(productID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_alerts (
                alertID       INTEGER PRIMARY KEY AUTOINCREMENT,
                alertType     TEXT,
                severity      TEXT    NOT NULL DEFAULT 'LOW'
                              CHECK (severity IN ('LOW','WARNING','HIGH','CRITICAL')),
                message       TEXT,
                entityType    TEXT,
                entityID      INTEGER,
                isResolved    INTEGER NOT NULL DEFAULT 0,
                resolvedByUserID INTEGER,
                resolvedAt    TEXT,
                anomalyID     INTEGER,
                createdAt     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (resolvedByUserID) REFERENCES users(userID)             ON DELETE SET NULL,
                FOREIGN KEY (anomalyID)        REFERENCES finance_anomalies(anomalyID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_suppliers (
                supplierID       INTEGER PRIMARY KEY AUTOINCREMENT,
                name             TEXT    NOT NULL,
                contact          TEXT,
                email            TEXT,
                avgLeadDays      REAL    NOT NULL DEFAULT 5,
                reliabilityScore REAL    NOT NULL DEFAULT 0.85
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_supplier_products (
                supplierProductID     INTEGER PRIMARY KEY AUTOINCREMENT,
                supplierID            INTEGER NOT NULL,
                productID             INTEGER NOT NULL,
                unitCostFromSupplier  REAL    NOT NULL DEFAULT 0.00,
                leadDays              INTEGER NOT NULL DEFAULT 7,
                isPreferred           INTEGER NOT NULL DEFAULT 0,
                lastOrderDate         TEXT,
                UNIQUE (supplierID, productID),
                FOREIGN KEY (supplierID) REFERENCES finance_suppliers(supplierID)  ON DELETE CASCADE,
                FOREIGN KEY (productID)  REFERENCES products(productID)             ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS warehouse_warehouses (
                warehouseID   INTEGER PRIMARY KEY AUTOINCREMENT,
                warehouseName TEXT    NOT NULL,
                location      TEXT    NOT NULL,
                contactEmail  TEXT,
                capacityLimit INTEGER,
                warehouseCode TEXT    UNIQUE,
                productLine   TEXT    NOT NULL DEFAULT 'General'
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_supplier_orders (
                supplierOrderID  INTEGER PRIMARY KEY AUTOINCREMENT,
                supplierID       INTEGER NOT NULL,
                productID        INTEGER NOT NULL,
                warehouseID      INTEGER NOT NULL,
                quantity         INTEGER NOT NULL DEFAULT 1,
                unitCostAtOrder  REAL    NOT NULL DEFAULT 0.00,
                status           TEXT    NOT NULL DEFAULT 'ORDERED'
                                 CHECK (status IN ('ORDERED','SHIPPED','DELIVERED','CANCELLED')),
                orderedAt        TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expectedAt       TEXT,
                deliveredAt      TEXT,
                createdByUserID  INTEGER,
                notes            TEXT,
                FOREIGN KEY (supplierID)      REFERENCES finance_suppliers(supplierID),
                FOREIGN KEY (productID)       REFERENCES products(productID),
                FOREIGN KEY (warehouseID)     REFERENCES warehouse_warehouses(warehouseID),
                FOREIGN KEY (createdByUserID) REFERENCES users(userID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS finance_settings (
                settingKey   TEXT PRIMARY KEY,
                settingValue TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS warehouse_inventory (
                inventoryID       INTEGER PRIMARY KEY AUTOINCREMENT,
                warehouseID       INTEGER NOT NULL,
                productID         INTEGER NOT NULL,
                quantityOnHand    INTEGER NOT NULL DEFAULT 0,
                minStockThreshold INTEGER NOT NULL DEFAULT 0,
                lastRestockDate   TEXT,
                reorderQuantity   INTEGER NOT NULL DEFAULT 0,
                lowStockFlag      INTEGER NOT NULL DEFAULT 0,
                supplierID        INTEGER,
                unitCost          REAL    NOT NULL DEFAULT 0.00,
                isActive          INTEGER NOT NULL DEFAULT 1,
                UNIQUE (warehouseID, productID),
                FOREIGN KEY (warehouseID) REFERENCES warehouse_warehouses(warehouseID) ON DELETE CASCADE,
                FOREIGN KEY (productID)   REFERENCES products(productID),
                FOREIGN KEY (supplierID)  REFERENCES finance_suppliers(supplierID)     ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS warehouse_stock_movements (
                movementID       INTEGER PRIMARY KEY AUTOINCREMENT,
                inventoryID      INTEGER NOT NULL,
                fromWarehouseID  INTEGER,
                toWarehouseID    INTEGER,
                quantityChanged  INTEGER NOT NULL,
                movementType     TEXT,
                performedByUserID INTEGER,
                movementDate     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (inventoryID)       REFERENCES warehouse_inventory(inventoryID)   ON DELETE CASCADE,
                FOREIGN KEY (fromWarehouseID)   REFERENCES warehouse_warehouses(warehouseID)  ON DELETE SET NULL,
                FOREIGN KEY (toWarehouseID)     REFERENCES warehouse_warehouses(warehouseID)  ON DELETE SET NULL,
                FOREIGN KEY (performedByUserID) REFERENCES users(userID)                      ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS warehouse_restock_requests (
                requestID         INTEGER PRIMARY KEY AUTOINCREMENT,
                warehouseID       INTEGER NOT NULL,
                productID         INTEGER NOT NULL,
                requestedQty      INTEGER NOT NULL,
                requestedByUserID INTEGER,
                status            TEXT    NOT NULL DEFAULT 'Pending'
                                  CHECK (status IN ('Pending','Approved','Fulfilled','Cancelled')),
                requestDate       TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                fulfilledDate     TEXT,
                FOREIGN KEY (warehouseID)       REFERENCES warehouse_warehouses(warehouseID) ON DELETE CASCADE,
                FOREIGN KEY (productID)         REFERENCES products(productID),
                FOREIGN KEY (requestedByUserID) REFERENCES users(userID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS warehouse_audit_log (
                auditID           INTEGER PRIMARY KEY AUTOINCREMENT,
                warehouseID       INTEGER,
                performedByUserID INTEGER,
                actionType        TEXT,
                description       TEXT,
                actionDate        TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (warehouseID)       REFERENCES warehouse_warehouses(warehouseID) ON DELETE SET NULL,
                FOREIGN KEY (performedByUserID) REFERENCES users(userID)                     ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS delivery_drivers (
                driverID       INTEGER PRIMARY KEY AUTOINCREMENT,
                licenceNumber  TEXT    UNIQUE,
                phoneNum       TEXT,
                email          TEXT,
                driverName     TEXT    NOT NULL,
                userID         INTEGER,
                status         TEXT    NOT NULL DEFAULT 'ACTIVE',
                FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS delivery_deliveries (
                deliveryID      INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID         INTEGER NOT NULL,
                customerAddress TEXT,
                orderStatus     TEXT    NOT NULL DEFAULT 'Pending'
                                CHECK (orderStatus IN ('Pending','Assigned','In Transit','Delivered','Rejected','Returned')),
                orderDate       TEXT,
                numOfItems      INTEGER NOT NULL DEFAULT 1,
                driverID        INTEGER,
                warehouseID     INTEGER,
                FOREIGN KEY (orderID)     REFERENCES orders(orderID)                     ON DELETE CASCADE,
                FOREIGN KEY (driverID)    REFERENCES delivery_drivers(driverID)          ON DELETE SET NULL,
                FOREIGN KEY (warehouseID) REFERENCES warehouse_warehouses(warehouseID)   ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS delivery_log (
                logID         INTEGER PRIMARY KEY AUTOINCREMENT,
                deliveryID    INTEGER NOT NULL,
                driverID      INTEGER,
                timeDelivered TEXT,
                statusChange  TEXT,
                logDate       TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (deliveryID) REFERENCES delivery_deliveries(deliveryID) ON DELETE CASCADE,
                FOREIGN KEY (driverID)   REFERENCES delivery_drivers(driverID)      ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS product_images (
                imageID    INTEGER PRIMARY KEY AUTOINCREMENT,
                productID  INTEGER NOT NULL,
                imageURL   TEXT    NOT NULL,
                fileType   TEXT,
                sizeKB     INTEGER,
                isPrimary  INTEGER NOT NULL DEFAULT 0,
                uploadedAt TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (productID) REFERENCES products(productID) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS product_validations (
                validationID      INTEGER PRIMARY KEY AUTOINCREMENT,
                productID         INTEGER NOT NULL,
                validatedByUserID INTEGER,
                validationDate    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                isValid           INTEGER,
                validationMessage TEXT,
                FOREIGN KEY (productID)         REFERENCES products(productID)  ON DELETE CASCADE,
                FOREIGN KEY (validatedByUserID) REFERENCES users(userID)        ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS product_categories (
                productID  INTEGER NOT NULL,
                categoryID INTEGER NOT NULL,
                PRIMARY KEY (productID, categoryID),
                FOREIGN KEY (productID)  REFERENCES products(productID)    ON DELETE CASCADE,
                FOREIGN KEY (categoryID) REFERENCES categories(categoryID) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orders_status_history (
                historyID       INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID         INTEGER NOT NULL,
                previousStatus  TEXT,
                newStatus       TEXT    NOT NULL,
                changedByUserID INTEGER,
                notes           TEXT,
                changedAt       TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (orderID)         REFERENCES orders(orderID)  ON DELETE CASCADE,
                FOREIGN KEY (changedByUserID) REFERENCES users(userID)    ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reviews_reviews (
                reviewID       INTEGER PRIMARY KEY AUTOINCREMENT,
                productID      INTEGER NOT NULL,
                customerID     INTEGER NOT NULL,
                rating         INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                comment        TEXT    NOT NULL,
                createdAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updatedAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                status         TEXT    NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE','FLAGGED','REMOVED','CUSTOMER_DELETED')),
                helpfulCount   INTEGER NOT NULL DEFAULT 0,
                unhelpfulCount INTEGER NOT NULL DEFAULT 0,
                UNIQUE (productID, customerID),
                FOREIGN KEY (productID)  REFERENCES products(productID)    ON DELETE CASCADE,
                FOREIGN KEY (customerID) REFERENCES customers(customerID)  ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reviews_votes (
                voteID     INTEGER PRIMARY KEY AUTOINCREMENT,
                reviewID   INTEGER NOT NULL,
                customerID INTEGER NOT NULL,
                voteType   TEXT    NOT NULL CHECK (voteType IN ('HELPFUL','UNHELPFUL')),
                votedAt    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (reviewID, customerID),
                FOREIGN KEY (reviewID)   REFERENCES reviews_reviews(reviewID)   ON DELETE CASCADE,
                FOREIGN KEY (customerID) REFERENCES customers(customerID)       ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reviews_moderation (
                auditID      INTEGER PRIMARY KEY AUTOINCREMENT,
                reviewID     INTEGER NOT NULL,
                adminUserID  INTEGER NOT NULL,
                action       TEXT    NOT NULL,
                reason       TEXT    NOT NULL,
                actionTime   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (reviewID)    REFERENCES reviews_reviews(reviewID) ON DELETE CASCADE,
                FOREIGN KEY (adminUserID) REFERENCES users(userID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reviews_settings (
                settingKey   TEXT PRIMARY KEY,
                settingValue TEXT NOT NULL
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql.trim());
            }
            System.out.println("Unified schema tables created/verified.");
        } catch (SQLException e) {
            System.err.println("createTables failed: " + e.getMessage());
        }
    }

    private void createIndexes() {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_users_email            ON users(email)",
            "CREATE INDEX IF NOT EXISTS idx_user_roles_user        ON user_roles(userID)",
            "CREATE INDEX IF NOT EXISTS idx_customers_email        ON customers(email)",
            "CREATE INDEX IF NOT EXISTS idx_customers_user         ON customers(userID)",
            "CREATE INDEX IF NOT EXISTS idx_products_sku           ON products(sku)",
            "CREATE INDEX IF NOT EXISTS idx_products_name          ON products(name)",
            "CREATE INDEX IF NOT EXISTS idx_products_category      ON products(categoryID)",
            "CREATE INDEX IF NOT EXISTS idx_orders_customer        ON orders(customerID)",
            "CREATE INDEX IF NOT EXISTS idx_orders_status          ON orders(status)",
            "CREATE INDEX IF NOT EXISTS idx_order_items_order      ON order_items(orderID)",
            "CREATE INDEX IF NOT EXISTS idx_payments_order         ON payments(orderID)",
            "CREATE INDEX IF NOT EXISTS idx_finance_invoices_customer ON finance_invoices(customerID)",
            "CREATE INDEX IF NOT EXISTS idx_finance_invoices_order    ON finance_invoices(orderID)",
            "CREATE INDEX IF NOT EXISTS idx_warehouse_inv_warehouse   ON warehouse_inventory(warehouseID)",
            "CREATE INDEX IF NOT EXISTS idx_warehouse_inv_product     ON warehouse_inventory(productID)",
            "CREATE INDEX IF NOT EXISTS idx_delivery_order            ON delivery_deliveries(orderID)",
            "CREATE INDEX IF NOT EXISTS idx_delivery_driver           ON delivery_deliveries(driverID)",
            "CREATE INDEX IF NOT EXISTS idx_reviews_product           ON reviews_reviews(productID)",
            "CREATE INDEX IF NOT EXISTS idx_reviews_customer          ON reviews_reviews(customerID)",
            "CREATE INDEX IF NOT EXISTS idx_reviews_status            ON reviews_reviews(status)",
            "CREATE INDEX IF NOT EXISTS idx_reviews_votes_review      ON reviews_votes(reviewID)",
            "CREATE INDEX IF NOT EXISTS idx_orders_history_order      ON orders_status_history(orderID)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : indexes) {
                stmt.execute(sql);
            }
            System.out.println("Schema indexes created/verified.");
        } catch (SQLException e) {
            System.err.println("createIndexes failed: " + e.getMessage());
        }
    }

    private void seedAll() {
        String adminHash = hashPassword("admin123");
        String customerHash = hashPassword("customer123");

        String insertRoles = """
            INSERT OR IGNORE INTO roles (roleName, description) VALUES
            ('super_admin', 'Full access to all modules'),
            ('customer', 'Registered customer — storefront access'),
            ('product_admin', 'Product module — catalogue and validations'),
            ('finance_user', 'Finance module — view reports'),
            ('finance_admin', 'Finance module — full finance control'),
            ('warehouse_user', 'Warehouse module — stock updates'),
            ('warehouse_admin', 'Warehouse module — full inventory control'),
            ('delivery_user', 'Delivery module — update delivery status'),
            ('delivery_admin', 'Delivery module — assign drivers and deliveries'),
            ('orders_user', 'Orders module — process orders'),
            ('orders_admin', 'Orders module — full order management'),
            ('reviews_admin', 'Reviews module — moderate reviews')
            """;

        String insertUsers = """
            INSERT OR IGNORE INTO users (email, username, passwordHash, firstName, lastName, isActive)
            VALUES (?, ?, ?, ?, ?, 1), (?, ?, ?, ?, ?, 1)
            """;

        try (Statement st = connection.createStatement()) {
            st.execute(insertRoles);
        } catch (SQLException e) {
            System.err.println("seedAll roles failed: " + e.getMessage());
            return;
        }

        try (PreparedStatement ps = connection.prepareStatement(insertUsers)) {
            ps.setString(1, "admin@raez.com");
            ps.setString(2, "admin");
            ps.setString(3, adminHash);
            ps.setString(4, "Admin");
            ps.setString(5, "User");
            ps.setString(6, "customer@example.com");
            ps.setString(7, "customer");
            ps.setString(8, customerHash);
            ps.setString(9, "Customer");
            ps.setString(10, "User");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("seedAll users failed: " + e.getMessage());
            return;
        }

        int adminUserId;
        int customerUserId;
        int productAdminRoleId;
        int customerRoleId;
        try (Statement st = connection.createStatement();
             var rsUsers = st.executeQuery(
                 "SELECT userID FROM users WHERE email = 'admin@raez.com'")) {
            if (!rsUsers.next()) {
                System.err.println("seedAll: admin user not found");
                return;
            }
            adminUserId = rsUsers.getInt(1);
        } catch (SQLException e) {
            System.err.println("seedAll user id lookup failed: " + e.getMessage());
            return;
        }

        try (Statement st = connection.createStatement();
             var rsCust = st.executeQuery(
                 "SELECT userID FROM users WHERE email = 'customer@example.com'")) {
            if (!rsCust.next()) {
                System.err.println("seedAll: customer user not found");
                return;
            }
            customerUserId = rsCust.getInt(1);
        } catch (SQLException e) {
            System.err.println("seedAll customer user id lookup failed: " + e.getMessage());
            return;
        }

        try (Statement st = connection.createStatement();
             var rsRoles = st.executeQuery(
                 "SELECT roleID, roleName FROM roles WHERE roleName IN ('product_admin','customer')")) {
            productAdminRoleId = 0;
            customerRoleId = 0;
            while (rsRoles.next()) {
                if ("product_admin".equals(rsRoles.getString(2))) productAdminRoleId = rsRoles.getInt(1);
                if ("customer".equals(rsRoles.getString(2))) customerRoleId = rsRoles.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("seedAll role id lookup failed: " + e.getMessage());
            return;
        }

        String insertUserRoles =
            "INSERT OR IGNORE INTO user_roles (userID, roleID) VALUES (?, ?), (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertUserRoles)) {
            ps.setInt(1, adminUserId);
            ps.setInt(2, productAdminRoleId);
            ps.setInt(3, customerUserId);
            ps.setInt(4, customerRoleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("seedAll user_roles failed: " + e.getMessage());
            return;
        }

        String insertCustomer = """
            INSERT OR IGNORE INTO customers (userID, name, email, contactNumber, deliveryAddress, customerType, status)
            VALUES (?, ?, ?, NULL, NULL, 'Individual', 'active')
            """;
        try (PreparedStatement ps = connection.prepareStatement(insertCustomer)) {
            ps.setInt(1, customerUserId);
            ps.setString(2, "Customer User");
            ps.setString(3, "customer@example.com");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("seedAll customers failed: " + e.getMessage());
            return;
        }

        String insertCategories = """
            INSERT OR IGNORE INTO categories (categoryName, description, isActive) VALUES
            ('Electronics', 'Electronic goods and gadgets', 1),
            ('Toys & Games', 'Toys, games, and play', 1),
            ('Home & Living', 'Home and lifestyle products', 1),
            ('Educational', 'Learning and educational items', 1)
            """;
        try (Statement st = connection.createStatement()) {
            st.execute(insertCategories);
        } catch (SQLException e) {
            System.err.println("seedAll categories failed: " + e.getMessage());
            return;
        }

        int catElectronics;
        int catToys;
        int catHome;
        try (Statement st = connection.createStatement();
             var rs = st.executeQuery(
                 "SELECT categoryID, categoryName FROM categories ORDER BY categoryID")) {
            catElectronics = 0;
            catToys = 0;
            catHome = 0;
            while (rs.next()) {
                String n = rs.getString(2);
                if ("Electronics".equals(n)) catElectronics = rs.getInt(1);
                if ("Toys & Games".equals(n)) catToys = rs.getInt(1);
                if ("Home & Living".equals(n)) catHome = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("seedAll category ids failed: " + e.getMessage());
            return;
        }

        String insertProducts = """
            INSERT OR IGNORE INTO products (sku, name, description, price, unitCost, status, categoryID) VALUES
            (?, ?, ?, ?, ?, 'active', ?),
            (?, ?, ?, ?, ?, 'active', ?),
            (?, ?, ?, ?, ?, 'active', ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(insertProducts)) {
            ps.setString(1, "SKU-EL-1001");
            ps.setString(2, "Nova Bluetooth Speaker");
            ps.setString(3, "Portable 360° sound with 12-hour battery");
            ps.setDouble(4, 79.99);
            ps.setDouble(5, 42.50);
            ps.setInt(6, catElectronics);

            ps.setString(7, "SKU-TG-2002");
            ps.setString(8, "STEM Robotics Kit");
            ps.setString(9, "Build-and-code robot set for ages 10+");
            ps.setDouble(10, 129.00);
            ps.setDouble(11, 68.00);
            ps.setInt(12, catToys);

            ps.setString(13, "SKU-HL-3003");
            ps.setString(14, "Aroma Smart Diffuser");
            ps.setString(15, "Wi-Fi humidifier with app scheduling");
            ps.setDouble(16, 59.50);
            ps.setDouble(17, 31.25);
            ps.setInt(18, catHome);

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("seedAll products failed: " + e.getMessage());
            return;
        }

        int p1;
        int p2;
        int p3;
        try (Statement st = connection.createStatement();
             var rs = st.executeQuery("""
                 SELECT productID FROM products WHERE sku IN ('SKU-EL-1001','SKU-TG-2002','SKU-HL-3003')
                 ORDER BY CASE sku
                   WHEN 'SKU-EL-1001' THEN 1
                   WHEN 'SKU-TG-2002' THEN 2
                   WHEN 'SKU-HL-3003' THEN 3 END
                 """)) {
            if (!rs.next()) {
                System.err.println("seedAll: no seeded products found");
                return;
            }
            p1 = rs.getInt(1);
            if (!rs.next()) {
                System.err.println("seedAll: expected 3 products");
                return;
            }
            p2 = rs.getInt(1);
            if (!rs.next()) {
                System.err.println("seedAll: expected 3 products");
                return;
            }
            p3 = rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("seedAll product ids failed: " + e.getMessage());
            return;
        }

        String insertWarehouse = """
            INSERT OR IGNORE INTO warehouse_warehouses (warehouseName, location, warehouseCode, productLine)
            VALUES ('Main Warehouse', 'London, UK', 'WH-001', 'General')
            """;
        int warehouseId;
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(insertWarehouse);
            try (var rs = st.executeQuery(
                "SELECT warehouseID FROM warehouse_warehouses WHERE warehouseCode = 'WH-001'")) {
                if (!rs.next()) {
                    System.err.println("seedAll: warehouse WH-001 not found");
                    return;
                }
                warehouseId = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("seedAll warehouse failed: " + e.getMessage());
            return;
        }

        String insertInv = """
            INSERT OR IGNORE INTO warehouse_inventory
            (warehouseID, productID, quantityOnHand, minStockThreshold, reorderQuantity, unitCost, isActive)
            VALUES (?, ?, 50, 10, 0, ?, 1), (?, ?, 50, 10, 0, ?, 1), (?, ?, 50, 10, 0, ?, 1)
            """;
        try (PreparedStatement ps = connection.prepareStatement(insertInv)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, p1);
            ps.setDouble(3, 42.50);
            ps.setInt(4, warehouseId);
            ps.setInt(5, p2);
            ps.setDouble(6, 68.00);
            ps.setInt(7, warehouseId);
            ps.setInt(8, p3);
            ps.setDouble(9, 31.25);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("seedAll warehouse_inventory failed: " + e.getMessage());
        }

        System.out.println("seedAll completed.");
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
