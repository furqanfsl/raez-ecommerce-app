-- ================================================================
--  RAEZ UNIFIED DATABASE SCHEMA
--  Version : 1.0
--  Modules  : Customer | Finance | Warehouse | Product
--             Delivery | Orders (new) | Reviews
--  Normal form: 3NF throughout
--  Engine   : SQLite 3
-- ================================================================

PRAGMA foreign_keys = ON;
PRAGMA journal_mode  = WAL;

-- ================================================================
--  SECTION 1 — SHARED : AUTHENTICATION & ROLES
--  Replaces: FUser, fuser, WarehouseStaff, WarehouseUser,
--            admins (reviews), LoginCredentials, WarehouseUser,
--            roles (finance), role_permissions (all modules)
-- ================================================================

CREATE TABLE IF NOT EXISTS roles (
    roleID      INTEGER PRIMARY KEY AUTOINCREMENT,
    roleName    TEXT    NOT NULL UNIQUE,
    -- Valid values: super_admin | customer |
    --   finance_user | finance_admin |
    --   warehouse_user | warehouse_admin |
    --   delivery_user  | delivery_admin  |
    --   product_admin  | orders_user | orders_admin |
    --   reviews_admin
    description TEXT,
    createdAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
);

-- One user can carry multiple roles (e.g. product_admin + reviews_admin)
CREATE TABLE IF NOT EXISTS user_roles (
    userRoleID INTEGER PRIMARY KEY AUTOINCREMENT,
    userID     INTEGER NOT NULL,
    roleID     INTEGER NOT NULL,
    assignedAt TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (userID, roleID),
    FOREIGN KEY (userID) REFERENCES users(userID)   ON DELETE CASCADE,
    FOREIGN KEY (roleID) REFERENCES roles(roleID)   ON DELETE CASCADE
);

-- Shared across all modules that need password reset
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    tokenID    INTEGER PRIMARY KEY AUTOINCREMENT,
    userID     INTEGER NOT NULL,
    token      TEXT    NOT NULL UNIQUE,
    expiryTime TEXT    NOT NULL,
    isUsed     INTEGER NOT NULL DEFAULT 0,
    createdAt  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
);

-- ================================================================
--  SECTION 2 — SHARED : CUSTOMERS
--  Replaces: CustomerRegistration (customer/finance/warehouse),
--            customers (reviews), customer_registration (products),
--            AdminUser (all modules — now just role super_admin in users)
--  NOTE: userID is NULL for B2B / guest customers with no login
-- ================================================================

CREATE TABLE IF NOT EXISTS customers (
    customerID    INTEGER PRIMARY KEY AUTOINCREMENT,
    userID        INTEGER,                        -- NULL for B2B / guest
    name          TEXT    NOT NULL,
    email         TEXT    NOT NULL UNIQUE,
    contactNumber TEXT,
    deliveryAddress TEXT,                         -- default shipping address
    customerType  TEXT    NOT NULL DEFAULT 'Individual',
    -- Values: Individual | Company
    idCardImage   TEXT,
    status        TEXT    NOT NULL DEFAULT 'active',
    registeredAt  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS customer_preferences (
    preferenceID          INTEGER PRIMARY KEY AUTOINCREMENT,
    customerID            INTEGER NOT NULL UNIQUE,
    preferredCategories   TEXT,
    notificationSettings  TEXT,
    deliveryInstructions  TEXT,
    FOREIGN KEY (customerID) REFERENCES customers(customerID) ON DELETE CASCADE
);

-- Audit log: admin changes to customer records
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
);

-- ================================================================
--  SECTION 3 — SHARED : PRODUCTS & CATEGORIES
--  Replaces: Product (customer/finance/warehouse), product (products),
--            Category (customer/finance/warehouse), category (products),
--            categories (finance)
--  3NF NOTE: stock column REMOVED from products.
--            Stock lives solely in warehouse_inventory.
--            Queries must JOIN warehouse_inventory to get stock level.
-- ================================================================

CREATE TABLE IF NOT EXISTS categories (
    categoryID   INTEGER PRIMARY KEY AUTOINCREMENT,
    categoryName TEXT    NOT NULL UNIQUE,
    description  TEXT,
    parentID     INTEGER,
    isActive     INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (parentID) REFERENCES categories(categoryID) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS products (
    productID   INTEGER PRIMARY KEY AUTOINCREMENT,
    sku         TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    description TEXT,
    price       REAL    NOT NULL DEFAULT 0.00,
    unitCost    REAL    NOT NULL DEFAULT 0.00,
    status      TEXT    NOT NULL DEFAULT 'active',
    -- Values: active | inactive | discontinued
    categoryID  INTEGER,
    collection  TEXT,
    -- Marketing collection name (e.g. 'Apex Automata', 'Sentinel Force',
    -- 'NovaMind', 'TerraCore'). Nullable for ungrouped products.
    createdAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoryID) REFERENCES categories(categoryID) ON DELETE SET NULL
);

-- ================================================================
--  SECTION 4 — SHARED : ORDERS & PAYMENTS
--  Replaces: Order (customer/finance/warehouse), orders (product/reviews),
--            OrderItem, order_items, order_item,
--            Payment, payment
-- ================================================================

CREATE TABLE IF NOT EXISTS orders (
    orderID     INTEGER PRIMARY KEY AUTOINCREMENT,
    customerID  INTEGER NOT NULL,
    orderDate   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalAmount REAL    NOT NULL DEFAULT 0.00,
    status      TEXT    NOT NULL DEFAULT 'Processing',
    -- Values: Processing | Confirmed | Picking | Shipped | Delivered | Cancelled | Refunded
    FOREIGN KEY (customerID) REFERENCES customers(customerID)
);

CREATE TABLE IF NOT EXISTS order_items (
    orderItemID INTEGER PRIMARY KEY AUTOINCREMENT,
    orderID     INTEGER NOT NULL,
    productID   INTEGER NOT NULL,
    quantity    INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unitPrice   REAL    NOT NULL DEFAULT 0.00,
    FOREIGN KEY (orderID)   REFERENCES orders(orderID)     ON DELETE CASCADE,
    FOREIGN KEY (productID) REFERENCES products(productID)
);

CREATE TABLE IF NOT EXISTS payments (
    paymentID      INTEGER PRIMARY KEY AUTOINCREMENT,
    orderID        INTEGER NOT NULL,
    amountPaid     REAL    NOT NULL,
    currency       TEXT    NOT NULL DEFAULT 'GBP',
    paymentMethod  TEXT,
    paymentStatus  TEXT    NOT NULL DEFAULT 'PENDING',
    -- Values: PENDING | COMPLETED | FAILED | REFUNDED
    transactionRef TEXT    UNIQUE,
    paymentDate    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes          TEXT,
    createdAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (orderID) REFERENCES orders(orderID)
);

-- ================================================================
--  SECTION 5 — FINANCE MODULE
--  Replaces: Invoice, Refund, FinancialAnomalies, Alert,
--            Supplier / suppliers, SupplierProduct,
--            SupplierOrder, GlobalSettings / settings
--  Dropped : AdminUser (now just super_admin role in users)
--            LoginCredentials (auth in users)
-- ================================================================

CREATE TABLE IF NOT EXISTS finance_invoices (
    invoiceID     INTEGER PRIMARY KEY AUTOINCREMENT,
    orderID       INTEGER,                        -- NULL for standalone invoices
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
);

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
);

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
);

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
);

CREATE TABLE IF NOT EXISTS finance_suppliers (
    supplierID       INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT    NOT NULL,
    contact          TEXT,
    email            TEXT,
    avgLeadDays      REAL    NOT NULL DEFAULT 5,
    reliabilityScore REAL    NOT NULL DEFAULT 0.85
);

-- Which supplier supplies which product and at what cost
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
);

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
);

CREATE TABLE IF NOT EXISTS finance_settings (
    settingKey   TEXT PRIMARY KEY,
    settingValue TEXT NOT NULL
);

-- ================================================================
--  SECTION 6 — WAREHOUSE MODULE
--  Replaces: Warehouse / warehouse (all modules),
--            InventoryRecord / inventory_record / inventory,
--            StockMovement / stock_movement,
--            RestockRequest,
--            WarehouseAuditLog
--  Dropped : WarehouseStaff (now users with warehouse_admin/user role)
--            WarehouseUser  (merged into users)
-- ================================================================

CREATE TABLE IF NOT EXISTS warehouse_warehouses (
    warehouseID   INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouseName TEXT    NOT NULL,
    location      TEXT    NOT NULL,
    contactEmail  TEXT,
    capacityLimit INTEGER,
    warehouseCode TEXT    UNIQUE,
    productLine   TEXT    NOT NULL DEFAULT 'General'
);

CREATE TABLE IF NOT EXISTS warehouse_inventory (
    inventoryID       INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouseID       INTEGER NOT NULL,
    productID         INTEGER NOT NULL,
    quantityOnHand    INTEGER NOT NULL DEFAULT 0,
    minStockThreshold INTEGER NOT NULL DEFAULT 0,
    lastRestockDate   TEXT,
    reorderQuantity   INTEGER NOT NULL DEFAULT 0,
    lowStockFlag      INTEGER NOT NULL DEFAULT 0,
    supplierID        INTEGER,                    -- preferred supplier for this item
    unitCost          REAL    NOT NULL DEFAULT 0.00,
    isActive          INTEGER NOT NULL DEFAULT 1,
    UNIQUE (warehouseID, productID),
    FOREIGN KEY (warehouseID) REFERENCES warehouse_warehouses(warehouseID) ON DELETE CASCADE,
    FOREIGN KEY (productID)   REFERENCES products(productID),
    FOREIGN KEY (supplierID)  REFERENCES finance_suppliers(supplierID)     ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS warehouse_stock_movements (
    movementID       INTEGER PRIMARY KEY AUTOINCREMENT,
    inventoryID      INTEGER NOT NULL,
    fromWarehouseID  INTEGER,
    toWarehouseID    INTEGER,
    quantityChanged  INTEGER NOT NULL,
    movementType     TEXT,
    -- Values: STOCK_IN | SALE | TRANSFER | DAMAGED | ADJUSTMENT
    performedByUserID INTEGER,
    movementDate     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventoryID)       REFERENCES warehouse_inventory(inventoryID)   ON DELETE CASCADE,
    FOREIGN KEY (fromWarehouseID)   REFERENCES warehouse_warehouses(warehouseID)  ON DELETE SET NULL,
    FOREIGN KEY (toWarehouseID)     REFERENCES warehouse_warehouses(warehouseID)  ON DELETE SET NULL,
    FOREIGN KEY (performedByUserID) REFERENCES users(userID)                      ON DELETE SET NULL
);

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
);

CREATE TABLE IF NOT EXISTS warehouse_audit_log (
    auditID           INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouseID       INTEGER,
    performedByUserID INTEGER,
    actionType        TEXT,
    description       TEXT,
    actionDate        TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouseID)       REFERENCES warehouse_warehouses(warehouseID) ON DELETE SET NULL,
    FOREIGN KEY (performedByUserID) REFERENCES users(userID)                     ON DELETE SET NULL
);

-- ================================================================
--  SECTION 7 — DELIVERY MODULE
--  Replaces: Driver / driver (all modules),
--            Delivery / delivery (all modules),
--            DeliveryLog / delivery_log
--  Dropped : LoginCredentials in delivery module (auth in users)
-- ================================================================

CREATE TABLE IF NOT EXISTS delivery_drivers (
    driverID       INTEGER PRIMARY KEY AUTOINCREMENT,
    licenceNumber  TEXT    UNIQUE,
    phoneNum       TEXT,
    email          TEXT,
    driverName     TEXT    NOT NULL,
    userID         INTEGER,   -- NULL if driver has no system login
    status         TEXT    NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE SET NULL
);

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
);

CREATE TABLE IF NOT EXISTS delivery_log (
    logID         INTEGER PRIMARY KEY AUTOINCREMENT,
    deliveryID    INTEGER NOT NULL,
    driverID      INTEGER,
    timeDelivered TEXT,
    statusChange  TEXT,
    logDate       TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deliveryID) REFERENCES delivery_deliveries(deliveryID) ON DELETE CASCADE,
    FOREIGN KEY (driverID)   REFERENCES delivery_drivers(driverID)      ON DELETE SET NULL
);

-- ================================================================
--  SECTION 8 — PRODUCT MODULE
--  Replaces: ProductImage / product_image,
--            ProductValidation / product_validation,
--            product_categories (junction)
-- ================================================================

CREATE TABLE IF NOT EXISTS product_images (
    imageID    INTEGER PRIMARY KEY AUTOINCREMENT,
    productID  INTEGER NOT NULL,
    imageURL   TEXT    NOT NULL,
    fileType   TEXT,
    sizeKB     INTEGER,
    isPrimary  INTEGER NOT NULL DEFAULT 0,
    uploadedAt TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (productID) REFERENCES products(productID) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_validations (
    validationID      INTEGER PRIMARY KEY AUTOINCREMENT,
    productID         INTEGER NOT NULL,
    validatedByUserID INTEGER,
    validationDate    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    isValid           INTEGER,
    validationMessage TEXT,
    FOREIGN KEY (productID)         REFERENCES products(productID)  ON DELETE CASCADE,
    FOREIGN KEY (validatedByUserID) REFERENCES users(userID)        ON DELETE SET NULL
);

-- Many-to-many: one product can belong to multiple categories
CREATE TABLE IF NOT EXISTS product_categories (
    productID  INTEGER NOT NULL,
    categoryID INTEGER NOT NULL,
    PRIMARY KEY (productID, categoryID),
    FOREIGN KEY (productID)  REFERENCES products(productID)    ON DELETE CASCADE,
    FOREIGN KEY (categoryID) REFERENCES categories(categoryID) ON DELETE CASCADE
);

-- ================================================================
--  SECTION 9 — ORDERS MODULE  (was MISSING — now added)
--  Core tables orders / order_items / payments live in Section 4.
--  This module's own table tracks the order lifecycle.
-- ================================================================

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
);

-- ================================================================
--  SECTION 10 — REVIEWS MODULE
--  Replaces: reviews (raez_reviews_db), review_votes,
--            moderation_audit, app_settings
--  Dropped : customers / admins / orders / order_items / products
--            tables from reviews_db (merged into shared tables)
--  3NF NOTE: average_rating and review_count removed from products.
--            Compute them at query time from reviews_reviews.
-- ================================================================

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
);

CREATE TABLE IF NOT EXISTS reviews_votes (
    voteID     INTEGER PRIMARY KEY AUTOINCREMENT,
    reviewID   INTEGER NOT NULL,
    customerID INTEGER NOT NULL,
    voteType   TEXT    NOT NULL CHECK (voteType IN ('HELPFUL','UNHELPFUL')),
    votedAt    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (reviewID, customerID),
    FOREIGN KEY (reviewID)   REFERENCES reviews_reviews(reviewID)   ON DELETE CASCADE,
    FOREIGN KEY (customerID) REFERENCES customers(customerID)       ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews_moderation (
    auditID      INTEGER PRIMARY KEY AUTOINCREMENT,
    reviewID     INTEGER NOT NULL,
    adminUserID  INTEGER NOT NULL,
    action       TEXT    NOT NULL,
    -- Values: FLAGGED | REMOVED | RESTORED | EDITED
    reason       TEXT    NOT NULL,
    actionTime   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reviewID)    REFERENCES reviews_reviews(reviewID) ON DELETE CASCADE,
    FOREIGN KEY (adminUserID) REFERENCES users(userID)
);

CREATE TABLE IF NOT EXISTS reviews_settings (
    settingKey   TEXT PRIMARY KEY,
    settingValue TEXT NOT NULL
);

-- ================================================================
--  SECTION 11 — CUSTOMER FAVOURITES
-- ================================================================

CREATE TABLE IF NOT EXISTS customer_favourites (
    favouriteID INTEGER PRIMARY KEY AUTOINCREMENT,
    customerID  INTEGER NOT NULL,
    productID   INTEGER NOT NULL,
    addedAt     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (customerID, productID),
    FOREIGN KEY (customerID) REFERENCES customers(customerID) ON DELETE CASCADE,
    FOREIGN KEY (productID)  REFERENCES products(productID)  ON DELETE CASCADE
);

-- ================================================================
--  INDEXES
-- ================================================================

CREATE INDEX IF NOT EXISTS idx_users_email            ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_roles_user        ON user_roles(userID);
CREATE INDEX IF NOT EXISTS idx_customers_email        ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_user         ON customers(userID);
CREATE INDEX IF NOT EXISTS idx_products_sku           ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name          ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category      ON products(categoryID);
CREATE INDEX IF NOT EXISTS idx_orders_customer        ON orders(customerID);
CREATE INDEX IF NOT EXISTS idx_orders_status          ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order      ON order_items(orderID);
CREATE INDEX IF NOT EXISTS idx_payments_order         ON payments(orderID);
CREATE INDEX IF NOT EXISTS idx_finance_invoices_customer ON finance_invoices(customerID);
CREATE INDEX IF NOT EXISTS idx_finance_invoices_order    ON finance_invoices(orderID);
CREATE INDEX IF NOT EXISTS idx_warehouse_inv_warehouse   ON warehouse_inventory(warehouseID);
CREATE INDEX IF NOT EXISTS idx_warehouse_inv_product     ON warehouse_inventory(productID);
CREATE INDEX IF NOT EXISTS idx_delivery_order            ON delivery_deliveries(orderID);
CREATE INDEX IF NOT EXISTS idx_delivery_driver           ON delivery_deliveries(driverID);
CREATE INDEX IF NOT EXISTS idx_reviews_product           ON reviews_reviews(productID);
CREATE INDEX IF NOT EXISTS idx_reviews_customer          ON reviews_reviews(customerID);
CREATE INDEX IF NOT EXISTS idx_reviews_status            ON reviews_reviews(status);
CREATE INDEX IF NOT EXISTS idx_reviews_votes_review      ON reviews_votes(reviewID);
CREATE INDEX IF NOT EXISTS idx_orders_history_order      ON orders_status_history(orderID);
CREATE INDEX IF NOT EXISTS idx_customer_favourites       ON customer_favourites(customerID);

-- ================================================================
--  SECTION 9 — SMTP SETTINGS (Super Admin configurable)
--  Single-row config table — settingID is always 1
-- ================================================================

CREATE TABLE IF NOT EXISTS smtp_settings (
    settingID    INTEGER PRIMARY KEY CHECK (settingID = 1),
    host         TEXT    NOT NULL DEFAULT '',
    port         INTEGER NOT NULL DEFAULT 587,
    username     TEXT    NOT NULL DEFAULT '',
    password     TEXT    NOT NULL DEFAULT '',
    fromAddress  TEXT    NOT NULL DEFAULT '',
    fromName     TEXT    NOT NULL DEFAULT 'RAEZ',
    useTls       INTEGER NOT NULL DEFAULT 1,
    isEnabled    INTEGER NOT NULL DEFAULT 0,
    updatedAt    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
