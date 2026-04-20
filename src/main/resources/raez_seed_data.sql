-- ================================================================
--  RAEZ UNIFIED DATABASE — SEED DATA
--  Run AFTER 1_unified_schema.sql
--  Covers all modules: Customer | Finance | Warehouse | Product
--                      Delivery | Orders | Reviews
-- ================================================================

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- ================================================================
--  ROLES
-- ================================================================
INSERT INTO roles (roleID, roleName, description) VALUES
 (1,  'super_admin',      'Full access to all modules'),
 (2,  'customer',         'Registered customer — storefront access only'),
 (3,  'finance_user',     'Finance module — view reports and invoices'),
 (4,  'finance_admin',    'Finance module — create, approve, manage all finance data'),
 (5,  'warehouse_user',   'Warehouse module — update stock and view inventory'),
 (6,  'warehouse_admin',  'Warehouse module — full inventory control and staff management'),
 (7,  'delivery_user',    'Delivery module — update delivery status'),
 (8,  'delivery_admin',   'Delivery module — assign drivers, manage deliveries'),
 (9,  'product_admin',    'Product module — manage catalogue, images, validations'),
 (10, 'orders_user',      'Orders module — view and process orders'),
 (11, 'orders_admin',     'Orders module — full order management and overrides'),
 (12, 'reviews_admin',    'Reviews module — moderate and manage reviews'),
 (13, 'customer_admin',   'Customer module — full admin access');

-- ================================================================
--  USERS
--  Passwords are bcrypt hashes.  Plaintext shown in comments only.
--  Plain: admin123, finance123, warehouse123, etc.
-- ================================================================
INSERT INTO users (userID, email, username, passwordHash, firstName, lastName, phone, isActive) VALUES
 -- Super admin  (password: admin123)
 (1,  'admin@raez.org.uk',          'superadmin',
      '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
      'System', 'Admin', NULL, 1),
 -- Finance users  (password: finance123 / raez123)
 (2,  'finance@raez.org.uk',        'finance_admin',
      '48f7312924d74358e75294e3b3613f2319d99e944184b69550f528577ca082fb',
      'Finance', 'Admin', NULL, 1),
 (3,  'j.carter@raez.org.uk',       'JCarter',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'John', 'Carter', '+44 99 12345678', 1),
 -- Warehouse users  (password: raez123)
 (4,  'admin@raez.org.uk.wh',       'wh_admin',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Warehouse', 'Admin', '+44 70 00000001', 1),
 (5,  'manager@raez.org.uk',        'wh_manager',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Warehouse', 'Manager', '+44 70 00000002', 1),
 (6,  'staff@raez.org.uk',          'wh_staff',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Warehouse', 'Staff', '+44 70 00000003', 1),
 -- Product admin  (password: raez123)
 (7,  'products@raez.org.uk',       'product_admin_user',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Product', 'Admin', NULL, 1),
 -- Delivery users  (password: raez123)
 (8,  'delivery@raez.org.uk',       'delivery_admin',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Delivery', 'Admin', NULL, 1),
 -- Orders users  (password: raez123)
 (9,  'orders@raez.org.uk',         'orders_admin',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Orders', 'Admin', NULL, 1),
 -- Reviews admin  (password: raez123)
 (10, 'reviews@raez.org.uk',        'reviews_admin_user',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Reviews', 'Admin', NULL, 1),
 -- Customer accounts (matching customers below)
 (11, 'alice@raez.com',             'alice_w',
      '4e40e8ffe0ee32fa53e139147ed559229a5930f89c2204706fc174beb36210b3',
      'Alice', 'Walker', NULL, 1),
 (12, 'omar@raez.com',              'omar_h',
      '56318228b3a39a2af341c080cc2d8b1d7e088ed24bd28d6cc9b34a8711253434',
      'Omar', 'Hassan', NULL, 1),
 (13, 'sara@raez.com',              'sara_m',
      '926b4b8a00cfab44b758450fa6bf188d4bf8541c2fd6b3d9b93d152d43a99f64',
      'Sara', 'Malik', NULL, 1),
 (14, 'maya@raez.com',              'maya_c',
      '3688058a6965c4c8e143d7002afb557fe910657ad819714abb0356c7551c84b7',
      'Maya', 'Chen', NULL, 1),
 (15, 'zaid@raez.com',              'zaid_n',
      '4eb84dcc7275bc750ea32fbfe061fc0477d7d332ed1071c1e06911ddec3a6556',
      'Zaid', 'Nasser', NULL, 1),
 -- Integration admin accounts
 (16, 'adminProduct@raez.org.uk',   'adminproduct',
      'b7eca4fe5d5f5c13536fa00d8e63e155dcf3db3c01be7d9d7cb9fa9100463c3a',
      'Product', 'Admin', NULL, 1),
 (17, 'adminCustomer@raez.org.uk',  'admincustomer',
      '51830a2250dbb5b97236ecfa4bd5bf4d4530a1d32e647d43791526e543b11f88',
      'Customer', 'Admin', NULL, 1);

-- ================================================================
--  USER ROLES
-- ================================================================
INSERT INTO user_roles (userID, roleID) VALUES
 (1,  1),  -- superadmin   → super_admin
 (2,  4),  -- finance@     → finance_admin
 (3,  3),  -- j.carter     → finance_user
 (4,  6),  -- wh_admin     → warehouse_admin
 (5,  6),  -- wh_manager   → warehouse_admin
 (6,  5),  -- wh_staff     → warehouse_user
 (7,  9),  -- products@    → product_admin
 (8,  8),  -- delivery@    → delivery_admin
 (9,  11), -- orders@      → orders_admin
 (10, 12), -- reviews@     → reviews_admin
 (11, 2),  -- alice        → customer
 (12, 2),  -- omar         → customer
 (13, 2),  -- sara         → customer
 (14, 2),  -- maya         → customer
 (15, 2),  -- zaid          → customer
 (16, 9),  -- adminProduct  → product_admin
 (17, 13); -- adminCustomer → customer_admin

-- ================================================================
--  CUSTOMERS
--  Includes B2B (no userID) from finance + individual online customers
-- ================================================================
INSERT INTO customers (customerID, userID, name, email, contactNumber, deliveryAddress, customerType, status) VALUES
 -- B2B customers (finance module — no system login)
 (1,  NULL, 'TechCorp Industries',   'procurement@techcorp.com',  '+44 20 1100 0001', 'London, UK',     'Company',    'active'),
 (2,  NULL, 'RoboManufacture Ltd',   'orders@robomfg.co.uk',      '+44 20 1100 0002', 'Birmingham, UK', 'Company',    'active'),
 (3,  NULL, 'Global Systems Plc',    'accounts@globalsystems.com','+44 20 1100 0003', 'Leeds, UK',      'Company',    'active'),
 (4,  NULL, 'Northwind Logistics',   'supply@northwind.com',      '+44 20 1100 0004', 'Manchester, UK', 'Company',    'active'),
 (5,  NULL, 'Innovate Holdings',     'finance@innovate.io',       '+44 20 1100 0005', 'Bristol, UK',    'Company',    'active'),
 -- Online individual customers (linked to users)
 (6,  11,   'Alice Walker',          'alice@raez.com',            NULL,               NULL,             'Individual', 'active'),
 (7,  12,   'Omar Hassan',           'omar@raez.com',             NULL,               NULL,             'Individual', 'active'),
 (8,  13,   'Sara Malik',            'sara@raez.com',             NULL,               NULL,             'Individual', 'active'),
 (9,  14,   'Maya Chen',             'maya@raez.com',             NULL,               NULL,             'Individual', 'active'),
 (10, 15,   'Zaid Nasser',           'zaid@raez.com',             NULL,               NULL,             'Individual', 'active');

-- ================================================================
--  CUSTOMER PREFERENCES
-- ================================================================
INSERT INTO customer_preferences (customerID, preferredCategories, notificationSettings, deliveryInstructions) VALUES
 (6, 'Home Assistants,Companions', 'EMAIL', 'Leave at door'),
 (7, 'Security Bots,Industrial',   'EMAIL', 'Ring doorbell'),
 (8, 'Educational',                'EMAIL', 'Leave with neighbour'),
 (9, 'Companions,Home Assistants', 'SMS',   'Call before delivery'),
 (10,'Industrial',                 'NONE',  NULL);

-- ================================================================
--  CUSTOMER UPDATES (audit sample)
-- ================================================================
INSERT INTO customer_updates (adminUserID, customerID, updatedField, oldValue, newValue) VALUES
 (1, 1, 'email', 'old@techcorp.com', 'procurement@techcorp.com');

-- ================================================================
--  CATEGORIES
-- ================================================================
INSERT INTO categories (categoryID, categoryName, description, isActive) VALUES
 (1, 'Home Assistants', 'Robots that help around the home',            1),
 (2, 'Security Bots',   'Robots for home and business security',       1),
 (3, 'Educational',     'Robots designed for learning',                1),
 (4, 'Companions',      'Social and companion robots',                 1),
 (5, 'Industrial',      'Industrial and professional robots',          1);

-- ================================================================
--  PRODUCTS  (active products merged from product_db + finance)
--  NOTE: stock column removed — see warehouse_inventory
-- ================================================================
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID) VALUES
 -- Finance products (robotics/industrial)
 (1,  'SKU-FIN-001', 'Quadcopter Drone Pro',       'Professional quadcopter drone',          3240.00, 2700.00, 'active', 5),
 (2,  'SKU-FIN-002', 'Hexapod Drone X6',           'Heavy-lift hexapod drone',               3840.00, 3200.00, 'active', 5),
 (3,  'SKU-FIN-003', 'Autonomous Scout Drone',     'AI-guided autonomous scouting drone',    3960.00, 3300.00, 'active', 5),
 (4,  'SKU-FIN-004', 'Delta Robot Cell',           'High-speed industrial delta robot',      3000.00, 2500.00, 'active', 5),
 (5,  'SKU-FIN-005', 'SCARA Robot Arm 500',        'Precision SCARA arm for assembly',       5640.00, 4700.00, 'active', 5),
 -- Product module (consumer/robot catalogue)
 (59, 'ARIA-2000',   'Aria Home Assistant',        'Advanced home assistant with AI',          299.99, 0.00,   'active', 1),
 (60, 'BOLT-X1',     'Bolt Security Bot',          'Security robot 360° camera',               599.99, 0.00,   'inactive',2),
 (65, 'GUARD-2',     'Guardian 2',                 'Next-gen home security bot',               899.99, 0.00,   'active', 2),
 (67, 'VERA-I',      'Vera Companion I',           'AI companion robot',                       449.99, 0.00,   'active', 4),
 (85, 'SKU-DF5A8781','Red Dead',                   'Test product',                              21.90, 0.00,   'active', 1),
 (109,'SKU-A0829569','Robot',                      'Robot product',                            300.00, 0.00,   'active', 5),
 (127,'SKU-64754EA8','New Product',                'Future of robots',                          12.00, 0.00,   'active', 2),
 (148,'SKU-C5384F96','New Robot',                  'Future of robots',                         200.00, 0.00,   'active', 1);

-- ================================================================
--  PRODUCT CATEGORIES  (many-to-many)
-- ================================================================
INSERT INTO product_categories (productID, categoryID) VALUES
 (59, 1), (60, 2), (65, 2), (67, 4), (85, 1), (85, 3),
 (109,5), (127,2), (127,4), (148,1);

-- ================================================================
--  PRODUCT IMAGES
-- ================================================================
INSERT INTO product_images (imageID, productID, imageURL, isPrimary) VALUES
 (1,  59,  'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400', 1),
 (2,  65,  'https://images.unsplash.com/photo-1518770660439-4636190af475?w=400', 1),
 (3,  67,  'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400', 1),
 (4,  85,  'https://images.unsplash.com/photo-1589254065909-b7086229d08c?w=600', 1),
 (5,  109, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400', 1);

-- ================================================================
--  PRODUCT VALIDATIONS
-- ================================================================
INSERT INTO product_validations (productID, validatedByUserID, isValid, validationMessage) VALUES
 (59,  7, 1, 'All product fields verified and images approved'),
 (65,  7, 1, 'Verified — passed quality check'),
 (85,  7, 0, 'Missing proper description — needs update'),
 (109, 7, 1, 'Approved for listing');

-- ================================================================
--  WAREHOUSE WAREHOUSES
-- ================================================================
INSERT INTO warehouse_warehouses (warehouseID, warehouseName, location, contactEmail, capacityLimit, warehouseCode, productLine) VALUES
 (1, 'Central Warehouse',           'London',   'central@raez.org.uk',  3000, 'WH-001', 'Robotics Parts'),
 (2, 'North Warehouse',             'Manchester','north@raez.org.uk',   3000, 'WH-002', 'Automation Equipment'),
 (3, 'South Warehouse',             'Birmingham','south@raez.org.uk',   3300, 'WH-003', 'Mixed Robotics Items'),
 (4, 'Uxbridge Warehouse',          'Uxbridge',  NULL,                  3783, 'WH-004', 'General'),
 (5, 'Brunel University Warehouse', 'Uxbridge',  NULL,                  3000, 'WH-005', 'General');

-- ================================================================
--  WAREHOUSE INVENTORY
-- ================================================================
INSERT INTO warehouse_inventory (warehouseID, productID, quantityOnHand, minStockThreshold, reorderQuantity, lowStockFlag, unitCost, isActive) VALUES
 (1, 1,   36, 5, 12, 0, 2700.00,   1),
 (1, 2,   37, 5, 12, 0, 3200.00,   1),
 (1, 3,    2, 5, 12, 1, 3300.00,   1),
 (1, 4,   39, 5, 12, 0, 2500.00,   1),
 (1, 5,   40, 5, 12, 0, 4700.00,   1),
 (1, 59,  15, 5, 10, 0,    0.00,   1),
 (1, 65,   6, 5, 10, 0,    0.00,   1),
 (1, 67,   9, 5, 10, 0,    0.00,   1),
 (1, 85,  33, 0,  0, 0,    0.00,   1),
 (2, 59,  12, 5, 10, 0,    0.00,   1);

-- ================================================================
--  FINANCE SUPPLIERS
-- ================================================================
INSERT INTO finance_suppliers (supplierID, name, contact, email, avgLeadDays, reliabilityScore) VALUES
 (1, 'AeroDynamics Ltd',    'James Price',   'jprice@aerodyn.com',    4, 0.92),
 (2, 'RoboCore Supplies',   'Lena Voss',     'lvoss@robocore.com',    5, 0.88),
 (3, 'TechParts Global',    'Sam Okafor',    'sokafor@techparts.com', 6, 0.85),
 (4, 'Precision Robotics',  'Aisha Malik',   'amalik@precisionr.com', 3, 0.95),
 (5, 'FastShip Components', 'Tom Huang',     'thuang@fastship.com',   2, 0.90);

-- ================================================================
--  FINANCE SUPPLIER PRODUCTS
-- ================================================================
INSERT INTO finance_supplier_products (supplierID, productID, unitCostFromSupplier, leadDays, isPreferred) VALUES
 (1, 1, 2700.00, 4, 1),
 (2, 2, 3200.00, 5, 1),
 (3, 3, 3300.00, 6, 1),
 (4, 4, 2500.00, 3, 1),
 (5, 5, 4700.00, 2, 1);

-- ================================================================
--  ORDERS  (10 sample orders)
-- ================================================================
INSERT INTO orders (orderID, customerID, orderDate, totalAmount, status) VALUES
 (1,  1, '2026-03-08 10:00:00', 5220.00,  'Delivered'),
 (2,  2, '2026-02-15 09:30:00', 495840.00,'Delivered'),
 (3,  3, '2026-01-22 14:00:00', 17400.00, 'Processing'),
 (4,  4, '2025-12-02 11:00:00', 47280.00, 'Delivered'),
 (5,  5, '2025-11-09 16:00:00', 74340.00, 'Delivered'),
 (6,  6, '2026-03-09 10:47:26', 239.49,   'Delivered'),
 (7,  7, '2026-03-12 10:47:26', 349.94,   'Delivered'),
 (8,  8, '2026-03-13 10:47:26', 249.00,   'Delivered'),
 (9,  9, '2026-03-07 12:11:18', 479.98,   'Delivered'),
 (10, 10,'2026-03-10 12:11:18', 409.98,   'Delivered');

-- ================================================================
--  ORDER ITEMS
-- ================================================================
INSERT INTO order_items (orderItemID, orderID, productID, quantity, unitPrice) VALUES
 (1,  1, 1,   1, 3240.00),
 (2,  1, 3,   1, 1980.00),
 (3,  2, 2,   2, 3840.00),
 (4,  2, 5,   1, 5640.00),
 (5,  3, 4,   1, 3000.00),
 (6,  3, 2,   1, 3840.00),
 (7,  6, 59,  1,  299.99),
 (8,  7, 65,  1,  899.99),
 (9,  8, 67,  1,  449.99),
 (10, 9, 67,  1,  449.99);

-- ================================================================
--  PAYMENTS
-- ================================================================
INSERT INTO payments (paymentID, orderID, amountPaid, currency, paymentMethod, paymentStatus, transactionRef, paymentDate) VALUES
 (1,  1, 5220.00,   'GBP', 'BANK_TRANSFER', 'COMPLETED', 'TXN-2026-0001', '2026-03-13 00:00:00'),
 (2,  2, 495840.00, 'GBP', 'BANK_TRANSFER', 'COMPLETED', 'TXN-2026-0002', '2026-02-20 00:00:00'),
 (3,  3, 17400.00,  'GBP', 'BANK_TRANSFER', 'PENDING',   'TXN-2026-0003', '2026-01-22 00:00:00'),
 (4,  4, 47280.00,  'GBP', 'BANK_TRANSFER', 'COMPLETED', 'TXN-2026-0004', '2025-12-02 00:00:00'),
 (5,  5, 74340.00,  'GBP', 'BANK_TRANSFER', 'COMPLETED', 'TXN-2026-0005', '2025-11-14 00:00:00'),
 (6,  6, 239.49,    'GBP', 'CARD',          'COMPLETED', 'TXN-2026-0006', '2026-03-09 00:00:00'),
 (7,  7, 349.94,    'GBP', 'CARD',          'COMPLETED', 'TXN-2026-0007', '2026-03-12 00:00:00'),
 (8,  8, 249.00,    'GBP', 'PAYPAL',        'COMPLETED', 'TXN-2026-0008', '2026-03-13 00:00:00'),
 (9,  9, 479.98,    'GBP', 'CARD',          'COMPLETED', 'TXN-2026-0009', '2026-03-07 00:00:00'),
 (10,10, 409.98,    'GBP', 'CARD',          'COMPLETED', 'TXN-2026-0010', '2026-03-10 00:00:00');

-- ================================================================
--  FINANCE INVOICES
-- ================================================================
INSERT INTO finance_invoices (invoiceID, orderID, customerID, paymentID, invoiceNumber, status, subtotal, vatAmount, totalAmount, currency, issuedAt, dueDate, paidAt, notes) VALUES
 (1, 1, 1, 1, 'INV-2026-0001', 'PAID',    4350.00,    870.00,    5220.00,   'GBP', '2026-03-08', '2026-04-07', '2026-03-13', 'Auto-generated'),
 (2, 2, 2, 2, 'INV-2026-0002', 'PAID',    413200.00,  82640.00,  495840.00, 'GBP', '2026-02-15', '2026-03-17', '2026-02-20', 'Auto-generated'),
 (3, 3, 3, 3, 'INV-2026-0003', 'PENDING', 14500.00,   2900.00,   17400.00,  'GBP', '2026-01-22', '2026-02-21',  NULL,        'Awaiting payment'),
 (4, 4, 4, 4, 'INV-2026-0004', 'OVERDUE', 39400.00,   7880.00,   47280.00,  'GBP', '2025-12-02', '2026-01-01',  NULL,        'Past due date'),
 (5, 5, 5, 5, 'INV-2026-0005', 'PAID',    61950.00,   12390.00,  74340.00,  'GBP', '2025-11-09', '2025-12-09', '2025-11-14', 'Auto-generated');

-- ================================================================
--  FINANCE ANOMALIES
-- ================================================================
INSERT INTO finance_anomalies (anomalyID, anomalyType, description, severity, detectionRule, isResolved, affectedCustomerID, affectedOrderID, affectedProductID) VALUES
 (1, 'ANOMALY_LOW_STOCK',   'Critical stock level detected for product',    'CRITICAL', 'RULE_STOCK_01', 0, NULL, NULL, 3),
 (2, 'ANOMALY_REFUND_SPIKE','Refund ratio exceeded threshold',              'HIGH',     'RULE_REFUND_02',0, 2,    NULL, NULL),
 (3, 'ANOMALY_OVERDUE_INV', 'Multiple invoices overdue beyond due date',    'HIGH',     'RULE_INV_03',   0, 3,    3,    NULL);

-- ================================================================
--  FINANCE ALERTS
-- ================================================================
INSERT INTO finance_alerts (alertID, alertType, severity, message, entityType, entityID, isResolved, anomalyID) VALUES
 (1, 'Low Stock',      'WARNING',  'Three inventory items at or below reorder threshold', 'PRODUCT',  3, 0, 1),
 (2, 'Refund Spike',   'HIGH',     'Refund ratio exceeded threshold for customer segment','CUSTOMER', 5, 0, 2),
 (3, 'Invoice Overdue','CRITICAL', 'Multiple invoices are overdue beyond due date',       'INVOICE',  4, 0, 3);

-- ================================================================
--  FINANCE SUPPLIER ORDERS
-- ================================================================
INSERT INTO finance_supplier_orders (supplierID, productID, warehouseID, quantity, unitCostAtOrder, status, orderedAt, expectedAt, createdByUserID) VALUES
 (1, 1, 1, 20, 2700.00, 'DELIVERED', '2026-02-01 00:00:00', '2026-02-05 00:00:00', 2),
 (3, 3, 1, 15, 3300.00, 'ORDERED',   '2026-03-10 00:00:00', '2026-03-16 00:00:00', 2);

-- ================================================================
--  FINANCE REFUNDS
-- ================================================================
INSERT INTO finance_refunds (orderID, orderItemID, productID, refundAmount, reason, status, processedByUserID) VALUES
 (6, 7,  59, 299.99, 'Product damaged on arrival',    'APPROVED',  2),
 (8, 9,  67, 449.99, 'Customer changed mind',         'REQUESTED', NULL);

-- ================================================================
--  FINANCE SETTINGS
-- ================================================================
INSERT INTO finance_settings (settingKey, settingValue) VALUES
 ('vat_rate',                 '20.0'),
 ('company_name',             'RAEZ Finance Ltd'),
 ('currency_symbol',          '£'),
 ('session_timeout_minutes',  '30'),
 ('low_stock_threshold',      '5');

-- ================================================================
--  DELIVERY DRIVERS
-- ================================================================
INSERT INTO delivery_drivers (driverID, licenceNumber, phoneNum, email, driverName, status) VALUES
 (1, 'DL-001', '07111111111', 'john.driver@raez.org.uk',  'John Smith',  'ACTIVE'),
 (2, 'DL-002', '07222222222', 'sara.driver@raez.org.uk',  'Sara Jones',  'ACTIVE'),
 (3, 'DL-003', '07333333333', 'mike.driver@raez.org.uk',  'Mike Brown',  'ACTIVE'),
 (4, 'DL-004', '+44 7700 100001', 'james@raez.org.uk',    'James Wilson','ACTIVE'),
 (5, 'DL-005', '+44 7700 100002', 'sarah2@raez.org.uk',   'Sarah Connor','ACTIVE');

-- ================================================================
--  DELIVERY DELIVERIES
-- ================================================================
INSERT INTO delivery_deliveries (deliveryID, orderID, customerAddress, orderStatus, orderDate, numOfItems, driverID, warehouseID) VALUES
 (1, 1,  'London, UK',         'Delivered',   '2026-03-10', 2, 1, 1),
 (2, 2,  'Birmingham, UK',     'Delivered',   '2026-03-10', 3, 2, 2),
 (3, 3,  'Leeds, UK',          'In Transit',  '2026-03-22', 2, 3, 1),
 (4, 6,  '123 High St, London','Delivered',   '2026-03-09', 1, 1, 1),
 (5, 7,  '45 Baker St, Manch', 'Delivered',   '2026-03-12', 1, 2, 2),
 (6, 8,  '78 Park Lane, Bham', 'Delivered',   '2026-03-13', 1, 3, 3),
 (7, 9,  '12 King St, Leeds',  'Delivered',   '2026-03-07', 1, 1, 1),
 (8, 10, '99 Victoria, Bristol','Delivered',  '2026-03-10', 2, 2, 1);

-- ================================================================
--  DELIVERY LOG
-- ================================================================
INSERT INTO delivery_log (deliveryID, driverID, timeDelivered, statusChange, logDate) VALUES
 (1, 1, '2026-03-10 14:30:00', 'Delivered',  '2026-03-10 14:30:00'),
 (2, 2, '2026-03-10 16:00:00', 'Delivered',  '2026-03-10 16:00:00'),
 (4, 1, '2026-03-09 13:45:00', 'Delivered',  '2026-03-09 13:45:00'),
 (5, 2, '2026-03-12 11:00:00', 'Delivered',  '2026-03-12 11:00:00'),
 (6, 3, '2026-03-13 15:20:00', 'Delivered',  '2026-03-13 15:20:00');

-- ================================================================
--  WAREHOUSE RESTOCK REQUESTS
-- ================================================================
INSERT INTO warehouse_restock_requests (warehouseID, productID, requestedQty, requestedByUserID, status, requestDate) VALUES
 (1, 3, 50, 5, 'Pending',   '2026-03-14 16:33:55'),
 (2, 3, 40, 6, 'Pending',   '2026-03-14 16:33:55'),
 (1, 1, 20, 5, 'Approved',  '2026-03-10 09:00:00');

-- ================================================================
--  WAREHOUSE AUDIT LOG
-- ================================================================
INSERT INTO warehouse_audit_log (warehouseID, performedByUserID, actionType, description) VALUES
 (1, 4, 'WAREHOUSE_CREATED',  'Central warehouse created'),
 (1, 5, 'RESTOCK_REQUEST',    'Requested restock for product SKU-FIN-001'),
 (2, 6, 'TRANSFER',           'Stock moved from Central to North Warehouse'),
 (1, 4, 'INVENTORY_ADJUSTED', 'Manual correction: product 3 quantity corrected');

-- ================================================================
--  WAREHOUSE STOCK MOVEMENTS
-- ================================================================
INSERT INTO warehouse_stock_movements (inventoryID, fromWarehouseID, toWarehouseID, quantityChanged, movementType, performedByUserID) VALUES
 (1, NULL, 1,  50,  'STOCK_IN',  5),
 (6, NULL, 1,  -5,  'SALE',      NULL),
 (7, NULL, 1,  -3,  'SALE',      NULL),
 (3, NULL, 1,  -1,  'DAMAGED',   6);

-- ================================================================
--  ORDERS STATUS HISTORY  (Orders module — was missing)
-- ================================================================
INSERT INTO orders_status_history (orderID, previousStatus, newStatus, changedByUserID, notes) VALUES
 (1,  NULL,          'Processing', 9,    'Order placed'),
 (1,  'Processing',  'Confirmed',  9,    'Payment verified'),
 (1,  'Confirmed',   'Shipped',    9,    'Dispatched from Central Warehouse'),
 (1,  'Shipped',     'Delivered',  9,    'Confirmed delivery'),
 (3,  NULL,          'Processing', 9,    'Order placed'),
 (3,  'Processing',  'Confirmed',  9,    'Payment pending — awaiting bank transfer'),
 (6,  NULL,          'Processing', NULL, 'Customer order'),
 (6,  'Processing',  'Delivered',  9,    'Delivered to customer');

-- ================================================================
--  REVIEWS  (from raez_reviews_db — customerIDs remapped to new table)
-- ================================================================
INSERT INTO reviews_reviews (reviewID, productID, customerID, rating, comment, createdAt, updatedAt, status, helpfulCount, unhelpfulCount) VALUES
 (1,    59, 6,  5, 'Great sound and very comfortable',                              '2026-03-15 10:47:26', '2026-03-19 11:53:42', 'ACTIVE',  1, 0),
 (2,    59, 7,  4, 'Battery life is solid for daily study sessions',               '2026-03-17 10:47:26', '2026-03-19 11:55:10', 'ACTIVE',  1, 0),
 (3,    67, 8,  5, 'Makes smooth coffee and heats quickly',                        '2026-03-18 10:47:26', '2026-03-19 11:55:30', 'FLAGGED', 1, 0),
 (4,    65, 9,  5, 'Nice product — exceeded expectations',                         '2026-03-19 11:45:41', '2026-03-19 11:45:41', 'ACTIVE',  0, 0),
 (5,    85, 10, 4, 'Good product but took a while to arrive',                      '2026-03-20 09:00:00', '2026-03-20 09:00:00', 'ACTIVE',  2, 0);

-- ================================================================
--  REVIEWS VOTES
-- ================================================================
INSERT INTO reviews_votes (reviewID, customerID, voteType) VALUES
 (1, 8,  'HELPFUL'),
 (2, 6,  'HELPFUL'),
 (3, 6,  'HELPFUL'),
 (4, 7,  'HELPFUL'),
 (5, 6,  'HELPFUL'),
 (5, 7,  'HELPFUL');

-- ================================================================
--  REVIEWS MODERATION
-- ================================================================
INSERT INTO reviews_moderation (reviewID, adminUserID, action, reason, actionTime) VALUES
 (1, 10, 'RESTORED', 'Reviewed — no violations found',              '2026-03-19 11:53:42'),
 (3, 10, 'FLAGGED',  'Contains unverified promotional wording',     '2026-03-19 11:55:30');

-- ================================================================
--  REVIEWS SETTINGS
-- ================================================================
INSERT INTO reviews_settings (settingKey, settingValue) VALUES
 ('review_edit_window_minutes', '5'),
 ('max_flagged_before_removal', '3'),
 ('allow_guest_reviews',        'false');

COMMIT;
