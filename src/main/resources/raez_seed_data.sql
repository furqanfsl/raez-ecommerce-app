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
 -- Customer accounts  (password: raez123)
 (11, 'alice@raez.com',             'alice_w',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Alice', 'Walker', NULL, 1),
 (12, 'omar@raez.com',              'omar_h',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Omar', 'Hassan', NULL, 1),
 (13, 'sara@raez.com',              'sara_m',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Sara', 'Malik', NULL, 1),
 (14, 'maya@raez.com',              'maya_c',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Maya', 'Chen', NULL, 1),
 (15, 'zaid@raez.com',              'zaid_n',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Zaid', 'Nasser', NULL, 1),
 -- Integration admin accounts  (password: raez123)
 -- SHA-256("raez123") = 88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee
 (16, 'adminProduct@raez.org.uk',   'adminproduct',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Product', 'Admin', NULL, 1),
 (17, 'adminCustomer@raez.org.uk',  'admincustomer',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Customer', 'Admin', NULL, 1),
 (18, 'adminWarehouse@raez.org.uk', 'adminwarehouse',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Warehouse', 'Admin', NULL, 1),
 (19, 'adminDelivery@raez.org.uk',  'admindelivery',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Delivery', 'Admin', NULL, 1),
 (20, 'adminFinance@raez.org.uk',   'adminfinance',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Finance', 'Admin', NULL, 1),
 (21, 'adminReviews@raez.org.uk',   'adminreviews',
      '88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee',
      'Reviews', 'Admin', NULL, 1);

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
 (16, 9),  -- adminProduct   → product_admin
 (17, 13), -- adminCustomer  → customer_admin
 (18, 6),  -- adminWarehouse → warehouse_admin
 (19, 8),  -- adminDelivery  → delivery_admin
 (20, 4),  -- adminFinance   → finance_admin
 (21, 12); -- adminReviews   → reviews_admin

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
 (5, 'Industrial',      'Industrial and professional robots',          1),
 (6, 'Robots',          'Full-scale autonomous AI robots',              1),
 (7, 'Mini Robots',     'Compact and portable robots',                  1),
 (8, 'Accessories',     'Mounts, power packs, and add-ons',             1),
 (9, 'Services',        'Installation, maintenance and AI training',    1);

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
 (3, 3,  'Leeds, UK',          'Pending',     '2026-03-22', 2, 3, 1),
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

-- ================================================================
--  EXTENDED PRODUCT CATALOGUE  (Phase 4 — 12 realistic robots)
-- ================================================================
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID) VALUES
 (200, 'NOVA-H1',   'Nova Home AI',
      'Voice-activated home assistant robot with smart device integration, schedule management, reminders, and natural language understanding. Controls your lights, locks, and appliances seamlessly.',
      349.99, 0.00, 'active', 1),
 (201, 'KIRA-HK2',  'Kira Housekeeper Bot',
      'Autonomous household cleaning and organising robot with AI vision to navigate furniture, detect mess, and sort clutter. Handles vacuuming, mopping, and light tidying on a programmed schedule.',
      799.99, 0.00, 'active', 1),
 (202, 'SENT-P1',   'Sentinel Patrol Bot',
      'Autonomous indoor/outdoor security patrol robot with night-vision cameras, motion and heat detection, and real-time alert streaming to your phone. Can emit audible deterrents and lock doors.',
      1299.99, 0.00, 'active', 2),
 (203, 'EAGLE-S2',  'Eagle Eye Sentry',
      'Fixed-mount AI security unit with 360° panoramic view, facial recognition, and licence plate logging. Integrates with your existing CCTV infrastructure and sends instant breach notifications.',
      649.99, 0.00, 'active', 2),
 (204, 'EDUB-V3',   'EduBot Classroom AI',
      'Interactive educational robot designed for STEM learning at primary and secondary level. Supports coding, mathematics, and science curricula with guided challenges, quizzes, and project-based activities.',
      479.99, 0.00, 'active', 3),
 (205, 'STEM-R1',   'STEM Rover Jr.',
      'Programmable rover for students aged 8–16. Learn block-based and Python coding, robotics fundamentals, and engineering principles through 200+ guided challenges and open sandbox mode.',
      229.99, 0.00, 'active', 3),
 (206, 'BUDDY-C1',  'Buddy Companion Robot',
      'Cheerful AI companion with expressive LCD face display, multi-language conversation, emotion sensing, and long-term memory of your preferences. Ideal for adults seeking daily social engagement.',
      599.99, 0.00, 'active', 4),
 (207, 'ELDER-C2',  'ElderCare Companion AI',
      'Dedicated companion robot for elderly users — monitors vitals, issues medication reminders, detects falls, and can summon emergency services. Provides daily conversation and cognitive exercise.',
      899.99, 0.00, 'active', 4),
 (208, 'MIRA-K3',   'Mira Kitchen Assistant',
      'AI kitchen robot that tracks pantry inventory, suggests recipes, and assists with meal preparation via a precision robotic arm. Integrates with smart fridges and grocery delivery services.',
      549.99, 0.00, 'active', 1),
 (209, 'ATLAS-W4',  'Atlas Warehouse Bot',
      'High-endurance warehouse automation robot for picking, sorting, and inventory management at scale. Operates 20 hours per day, integrates with standard WMS systems, and navigates dynamic floor plans.',
      4200.00, 3500.00, 'active', 5),
 (210, 'NEXUS-AI',  'Nexus AI Security Hub',
      'Central AI coordination hub that orchestrates multiple security bots, cameras, and sensors into a unified threat-intelligence network. Includes anomaly detection and automated incident reporting.',
      1899.99, 0.00, 'active', 2),
 (211, 'KIDBOT-E1', 'KidBot Explorer',
      'Fun and educational robot for children aged 5–10. Teaches letters, numbers, shapes, and creative play through interactive storytelling, dance, and drawing activities. Completely safe and durable.',
      189.99, 0.00, 'active', 3);

-- Extended product → category links
INSERT INTO product_categories (productID, categoryID) VALUES
 (200, 1), (201, 1), (202, 2), (203, 2),
 (204, 3), (205, 3), (206, 4), (207, 4),
 (208, 1), (209, 5), (210, 2), (211, 3);

-- Extended product images
INSERT INTO product_images (imageID, productID, imageURL, isPrimary) VALUES
 (6,  200, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600', 1),
 (7,  201, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600',    1),
 (8,  202, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),
 (9,  203, 'https://images.unsplash.com/photo-1531746790731-6c087fecd65a?w=600', 1),
 (10, 204, 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=600', 1),
 (11, 205, 'https://images.unsplash.com/photo-1589254065909-b7086229d08c?w=600', 1),
 (12, 206, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600', 1),
 (13, 207, 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600', 1),
 (14, 208, 'https://images.unsplash.com/photo-1547592180-85f173990554?w=600',    1),
 (15, 209, 'https://images.unsplash.com/photo-1565689157206-0fddef7589a2?w=600', 1),
 (16, 210, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),
 (17, 211, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600', 1);

-- Extended warehouse inventory (all in Central Warehouse)
INSERT INTO warehouse_inventory (warehouseID, productID, quantityOnHand, minStockThreshold, reorderQuantity, lowStockFlag, unitCost, isActive) VALUES
 (1, 200, 25, 5, 10, 0, 0.00, 1),
 (1, 201, 12, 3,  8, 0, 0.00, 1),
 (1, 202,  8, 3,  6, 0, 0.00, 1),
 (1, 203, 18, 5, 10, 0, 0.00, 1),
 (1, 204, 20, 5, 10, 0, 0.00, 1),
 (1, 205, 35, 5, 15, 0, 0.00, 1),
 (1, 206, 15, 4,  8, 0, 0.00, 1),
 (1, 207, 10, 3,  6, 0, 0.00, 1),
 (1, 208, 22, 5, 10, 0, 0.00, 1),
 (1, 209,  6, 2,  4, 0, 3500.00, 1),
 (1, 210,  7, 2,  4, 0, 0.00, 1),
 (1, 211, 40, 8, 20, 0, 0.00, 1);

-- ================================================================
--  DELIVERED ORDERS FOR NEW PRODUCTS
--  These give demo customers the ability to write reviews.
-- ================================================================
INSERT INTO orders (orderID, customerID, orderDate, totalAmount, status) VALUES
 (11, 6,  '2026-03-22 10:00:00', 829.98,  'Delivered'),
 (12, 7,  '2026-03-23 11:30:00', 1899.98, 'Delivered'),
 (13, 8,  '2026-03-24 14:00:00', 1029.98, 'Delivered'),
 (14, 9,  '2026-03-25 09:15:00', 1449.98, 'Delivered'),
 (15, 10, '2026-03-26 16:00:00', 6099.99, 'Delivered');

INSERT INTO order_items (orderItemID, orderID, productID, quantity, unitPrice) VALUES
 (11, 11, 200, 1,  349.99),
 (12, 11, 204, 1,  479.99),
 (13, 12, 202, 1, 1299.99),
 (14, 12, 206, 1,  599.99),
 (15, 13, 205, 1,  229.99),
 (16, 13, 201, 1,  799.99),
 (17, 14, 207, 1,  899.99),
 (18, 14, 208, 1,  549.99),
 (19, 15, 209, 1, 4200.00),
 (20, 15, 210, 1, 1899.99);

INSERT INTO payments (paymentID, orderID, amountPaid, currency, paymentMethod, paymentStatus, transactionRef, paymentDate) VALUES
 (11, 11,  829.98, 'GBP', 'CARD',  'COMPLETED', 'TXN-2026-0011', '2026-03-22 00:00:00'),
 (12, 12, 1899.98, 'GBP', 'CARD',  'COMPLETED', 'TXN-2026-0012', '2026-03-23 00:00:00'),
 (13, 13, 1029.98, 'GBP', 'PAYPAL','COMPLETED', 'TXN-2026-0013', '2026-03-24 00:00:00'),
 (14, 14, 1449.98, 'GBP', 'CARD',  'COMPLETED', 'TXN-2026-0014', '2026-03-25 00:00:00'),
 (15, 15, 6099.99, 'GBP', 'BANK_TRANSFER', 'COMPLETED', 'TXN-2026-0015', '2026-03-26 00:00:00');

-- Delivery entries for new orders
INSERT INTO delivery_deliveries (deliveryID, orderID, customerAddress, orderStatus, orderDate, numOfItems, driverID, warehouseID) VALUES
 (9,  11, '14 Maple Ave, London',     'Delivered', '2026-03-22', 2, 1, 1),
 (10, 12, '88 Regent St, Manchester', 'Delivered', '2026-03-23', 2, 2, 1),
 (11, 13, '33 Elm Close, Birmingham', 'Delivered', '2026-03-24', 2, 3, 1),
 (12, 14, '7 Willow Rd, Bristol',     'Delivered', '2026-03-25', 2, 1, 1),
 (13, 15, '101 Castle St, Leeds',     'Delivered', '2026-03-26', 2, 2, 1);

-- Reviews for new products from customers who received them
INSERT INTO reviews_reviews (reviewID, productID, customerID, rating, comment, createdAt, updatedAt, status, helpfulCount, unhelpfulCount) VALUES
 (6,  200, 6, 5, 'Nova Home AI changed my mornings completely — it manages my lights, heating, and reminders without a single hiccup. Setup took under 10 minutes.',
      '2026-03-28 09:12:00', '2026-03-28 09:12:00', 'ACTIVE', 3, 0),
 (7,  202, 7, 5, 'The Sentinel Patrol Bot is outstanding. Night vision is crystal clear and the motion alerts arrive on my phone instantly. Worth every penny for peace of mind.',
      '2026-03-29 11:45:00', '2026-03-29 11:45:00', 'ACTIVE', 2, 0),
 (8,  205, 8, 4, 'My son is obsessed with the STEM Rover Jr. He has already finished 40 challenges in the first week. The Python mode is genuinely well-designed for beginners.',
      '2026-03-30 14:20:00', '2026-03-30 14:20:00', 'ACTIVE', 4, 0),
 (9,  207, 9, 5, 'We bought ElderCare for my grandmother and the medication reminders alone have been life-changing for our family. The fall detection gave us immediate peace of mind.',
      '2026-03-31 10:05:00', '2026-03-31 10:05:00', 'ACTIVE', 5, 0),
 (10, 209, 10, 4, 'Atlas Warehouse Bot integrated with our WMS in under a day. Picking accuracy improved immediately. Only minor issue is it is slow on tight corners — firmware update should address it.',
      '2026-04-01 08:30:00', '2026-04-01 08:30:00', 'ACTIVE', 2, 1);

-- ================================================================
--  PHASE-4 COLLECTIONS — 44 products across 4 flagship collections
--  Collections: Apex Automata, Sentinel Force, NovaMind, TerraCore
--  Categories : Robots (6), Mini Robots (7), Accessories (8), Services (9)
-- ================================================================

-- ── Apex Automata — Kinetic Series (IDs 300-310) ─────────────────
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID, collection) VALUES
 (300, 'APX-K01', 'Apex Kinetic One',
      'Flagship humanoid AI companion with dual-actuator limbs, facial expression display, and real-time environment mapping. The signature Kinetic Series model — built for homes, studios, and showcase spaces.',
      5999.99, 3800.00, 'active', 6, 'Apex Automata'),
 (301, 'APX-K02', 'Apex Kinetic Lite',
      'Compact variant of the flagship Apex Kinetic One. Desk-friendly footprint, full voice interaction, and lightweight articulated arms for light home tasks and presentations.',
      2499.99, 1500.00, 'active', 7, 'Apex Automata'),
 (302, 'APX-K03', 'Apex Arm Pro',
      'Stand-alone precision robotic arm from the Kinetic Series. 7 degrees of freedom, sub-millimetre repeatability, and natural-language task control for makers and designers.',
      3299.99, 2100.00, 'active', 6, 'Apex Automata'),
 (303, 'APX-K04', 'Apex Runner',
      'Four-legged outdoor running robot with terrain-adaptive gait and 45 minute runtime. Ideal for fitness companionship, delivery pilots, and outdoor demonstrations.',
      4799.99, 3000.00, 'active', 6, 'Apex Automata'),
 (304, 'APX-K05', 'Apex Explorer',
      'Rugged wheeled outdoor robot with LIDAR navigation, dust-proof chassis, and twin HD cameras. Built for large estates, farms, and private surveying.',
      3899.99, 2400.00, 'active', 6, 'Apex Automata'),
 (305, 'APX-K06', 'Apex Pocket',
      'Pocket-sized assistant with voice AI, reminders, and gesture control. Magnetic mount, seven-hour battery, and full smart-home integration in a device that fits in your palm.',
      299.99, 150.00, 'active', 7, 'Apex Automata'),
 (306, 'APX-K07', 'Kinetic Power Dock',
      'Premium inductive charging dock for the Apex Kinetic range. Auto-align magnetic contacts, status LED ring, and 120W fast-charge support.',
      179.99, 70.00, 'active', 8, 'Apex Automata'),
 (307, 'APX-K08', 'Kinetic Armor Shell',
      'Impact-resistant exoshell for Apex Kinetic robots. Aerospace-grade aluminium alloy, tool-less swap, and three colourways (Graphite, Ivory, Midnight Navy).',
      249.99, 110.00, 'active', 8, 'Apex Automata'),
 (308, 'APX-K09', 'Kinetic Install Service',
      'Certified engineer visit: unboxing, calibration, home network integration, safety walkthrough, and a two-hour owner training session. Includes 14-day phone support.',
      199.00, 80.00, 'active', 9, 'Apex Automata'),
 (309, 'APX-K10', 'Kinetic AI Training Pack',
      '12-month personalised AI training service — fine-tune your Apex robot to your preferences, home layout, daily routines, and voice. Remote onboarding plus quarterly tuning calls.',
      349.00, 120.00, 'active', 9, 'Apex Automata'),
 (310, 'APX-K11', 'Apex Kinetic Max',
      'Top-tier Kinetic variant with dual-core AI accelerator, premium soft-touch fabric exterior, and extended 9-hour runtime. The executive-class flagship.',
      7499.99, 4800.00, 'active', 6, 'Apex Automata');

-- ── Sentinel Force — Guardian Series (IDs 311-321) ───────────────
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID, collection) VALUES
 (311, 'SFG-01', 'Sentinel Guardian Alpha',
      'Flagship autonomous security patrol robot with thermal imaging, night vision, and facial recognition. Silent electric drive and rugged all-weather chassis.',
      3499.99, 2200.00, 'active', 6, 'Sentinel Force'),
 (312, 'SFG-02', 'Sentinel Mini Patrol',
      'Compact indoor patrol bot for offices, retail, and warehouses. 360° camera, two-way audio, and automatic docking for 24/7 operation.',
      1299.99, 800.00, 'active', 7, 'Sentinel Force'),
 (313, 'SFG-03', 'Sentinel Sky Drone',
      'Tethered aerial security drone with 20x zoom, heat signature tracking, and up to 4 hours continuous patrol at altitude.',
      4299.99, 2800.00, 'active', 6, 'Sentinel Force'),
 (314, 'SFG-04', 'Sentinel Perimeter Sentinel',
      'Stationary perimeter defence unit with AI threat classification, loudspeaker deterrent, and automatic escalation to monitoring services.',
      2199.99, 1400.00, 'active', 6, 'Sentinel Force'),
 (315, 'SFG-05', 'Sentinel K9 Patrol',
      'Quadruped patrol robot modelled for rough terrain. Stair-climbing, all-weather operation, and handler-pairing mode for guided sweeps.',
      5999.99, 3800.00, 'active', 6, 'Sentinel Force'),
 (316, 'SFG-06', 'Sentinel Pocket Sentry',
      'Portable personal security companion. Clip-on form factor, panic alert, GPS beacon, and silent capture of environment audio/video.',
      229.99, 110.00, 'active', 7, 'Sentinel Force'),
 (317, 'SFG-07', 'Guardian Mount Kit',
      'Industrial wall and ceiling mounting kit for Sentinel stationary units. Vibration-damped, concealed cabling, and vandal-proof fasteners.',
      149.99, 55.00, 'active', 8, 'Sentinel Force'),
 (318, 'SFG-08', 'Guardian Battery Extender',
      'Hot-swappable high-density battery pack. Doubles operational runtime for Sentinel patrol robots. Fits all Guardian Series chassis.',
      329.99, 140.00, 'active', 8, 'Sentinel Force'),
 (319, 'SFG-09', 'Guardian 24/7 Monitoring',
      'Around-the-clock human-supervised monitoring service. Alerts are triaged by Sentinel staff; verified incidents trigger your chosen response protocol.',
      89.00, 30.00, 'active', 9, 'Sentinel Force'),
 (320, 'SFG-10', 'Guardian Annual Maintenance',
      'Yearly on-site inspection, firmware update, full diagnostic, battery health check, and cleaning. Includes priority parts replacement queue.',
      249.00, 90.00, 'active', 9, 'Sentinel Force'),
 (321, 'SFG-11', 'Sentinel Vanguard Pro',
      'Executive-grade mobile security unit for high-value estates and events. AI threat prediction, integrated drone dispatch, and armoured chassis.',
      8499.99, 5400.00, 'active', 6, 'Sentinel Force');

-- ── NovaMind — Intelligence Series (IDs 322-332) ─────────────────
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID, collection) VALUES
 (322, 'NOV-01', 'NovaMind Scholar',
      'Flagship educational and research robot with multimodal reasoning, curriculum-linked lessons, and a reference library of 10,000 interactive experiments.',
      2299.99, 1400.00, 'active', 6, 'NovaMind'),
 (323, 'NOV-02', 'NovaMind Tutor',
      'One-on-one AI tutor that adapts to your pace and learning style. Supports maths, sciences, humanities, and programming from primary through to undergraduate level.',
      1499.99, 900.00, 'active', 6, 'NovaMind'),
 (324, 'NOV-03', 'NovaMind Mini Brain',
      'Pocket AI thinker — brainstorming partner, language translator, and reading companion in a compact screen-free form factor.',
      249.99, 110.00, 'active', 7, 'NovaMind'),
 (325, 'NOV-04', 'NovaMind Kids',
      'Child-safe educational companion for ages 4-10. Storytelling, phonics, maths games, and gentle bed-time routines — all moderated by parent-set guardrails.',
      199.99, 85.00, 'active', 7, 'NovaMind'),
 (326, 'NOV-05', 'NovaMind Labs Pro',
      'Professional-grade AI research assistant. Designed for universities and R&D labs — supports citation tracing, dataset exploration, and hypothesis generation.',
      3499.99, 2100.00, 'active', 6, 'NovaMind'),
 (327, 'NOV-06', 'NovaMind Workshop',
      'Maker-focused smart robot with hands-on project guides, soldering assistance, 3D-printing queue management, and CAD co-piloting.',
      1799.99, 1100.00, 'active', 6, 'NovaMind'),
 (328, 'NOV-07', 'NovaMind Pocket Classroom',
      'Tiny portable learning device that turns any room into a classroom. Projects interactive lessons onto walls and desks with mid-air gesture input.',
      429.99, 190.00, 'active', 7, 'NovaMind'),
 (329, 'NOV-08', 'NovaMind Sensor Pack',
      'Modular sensor bundle — temperature, humidity, gas, gyroscope, depth camera. Plug-and-play for all NovaMind robots via the NovaLink connector.',
      139.99, 50.00, 'active', 8, 'NovaMind'),
 (330, 'NOV-09', 'NovaMind Voice Module Pro',
      'Upgrade voice module with studio-quality six-mic array, noise cancellation, and lifelike neural speech synthesis in twelve languages.',
      199.99, 80.00, 'active', 8, 'NovaMind'),
 (331, 'NOV-10', 'NovaMind Curriculum Subscription',
      '12-month curriculum subscription with weekly new modules across STEM, humanities, and creative arts. Includes parent/teacher dashboards.',
      79.00, 25.00, 'active', 9, 'NovaMind'),
 (332, 'NOV-11', 'NovaMind Custom AI Tuning',
      'Personalised fine-tuning service. Our data scientists adapt NovaMind to your business, classroom, or research domain. Includes ongoing support.',
      899.00, 350.00, 'active', 9, 'NovaMind');

-- ── TerraCore — Industrial Series (IDs 333-343) ──────────────────
INSERT INTO products (productID, sku, name, description, price, unitCost, status, categoryID, collection) VALUES
 (333, 'TC-01', 'TerraCore Foundry',
      'Heavy-industrial foundry automation robot with thermal-rated manipulators for steel and casting lines. Built for 24/7 shift operation in harsh environments.',
      14999.99, 9500.00, 'active', 6, 'TerraCore'),
 (334, 'TC-02', 'TerraCore Welder Arm',
      'Six-axis industrial welding arm. MIG, TIG, and laser welding modes, with integrated seam-tracking and live weld-quality monitoring.',
      8999.99, 5800.00, 'active', 6, 'TerraCore'),
 (335, 'TC-03', 'TerraCore Loader',
      'Autonomous warehouse loader rated for pallets up to 1,500 kg. Dynamic routing, obstacle avoidance, and eight-hour continuous operation.',
      10999.99, 7000.00, 'active', 6, 'TerraCore'),
 (336, 'TC-04', 'TerraCore Surveyor Mini',
      'Portable site-surveying rover with LIDAR, GPS-RTK, and automated volumetric reporting. Packs into a single flight case.',
      4299.99, 2600.00, 'active', 7, 'TerraCore'),
 (337, 'TC-05', 'TerraCore Dockworker',
      'Logistics dock robot that moves containers, scans barcodes, and syncs with leading WMS platforms out of the box.',
      7499.99, 4700.00, 'active', 6, 'TerraCore'),
 (338, 'TC-06', 'TerraCore Heavy Lifter',
      'Forklift-class autonomous lifter with 3,000 kg capacity, computer-vision pallet detection, and safety-rated pedestrian awareness.',
      17999.99, 11500.00, 'active', 6, 'TerraCore'),
 (339, 'TC-07', 'TerraCore Industrial Tread Kit',
      'Swap-in industrial rubber/metal tread system for TerraCore rovers. Rated for slurry, gravel, and frozen surfaces.',
      449.99, 190.00, 'active', 8, 'TerraCore'),
 (340, 'TC-08', 'TerraCore Heat Shield',
      'High-temperature heat shield assembly for foundry and welding use. Rated to 650°C surface exposure.',
      699.99, 280.00, 'active', 8, 'TerraCore'),
 (341, 'TC-09', 'TerraCore Diagnostic Tool',
      'Field diagnostic tablet for industrial engineers. Full system telemetry, fault-replay, and over-the-air firmware flashing.',
      549.99, 210.00, 'active', 8, 'TerraCore'),
 (342, 'TC-10', 'TerraCore Site Install',
      'Full industrial site commissioning — safety audit, floor marking, fleet pairing, operator certification, and handover. Typical engagement: one week.',
      2499.00, 900.00, 'active', 9, 'TerraCore'),
 (343, 'TC-11', 'TerraCore Industrial Training',
      'Certified three-day on-site training programme for maintenance engineers and operators. Up to 10 trainees per cohort.',
      1299.00, 450.00, 'active', 9, 'TerraCore');

-- ── Collection → category links (many-to-many) ───────────────────
INSERT INTO product_categories (productID, categoryID) VALUES
 -- Apex Automata
 (300,6),(301,7),(302,6),(303,6),(304,6),(305,7),(306,8),(307,8),(308,9),(309,9),(310,6),
 -- Sentinel Force
 (311,6),(312,7),(313,6),(314,6),(315,6),(316,7),(317,8),(318,8),(319,9),(320,9),(321,6),
 -- NovaMind
 (322,6),(323,6),(324,7),(325,7),(326,6),(327,6),(328,7),(329,8),(330,8),(331,9),(332,9),
 -- TerraCore
 (333,6),(334,6),(335,6),(336,7),(337,6),(338,6),(339,8),(340,8),(341,8),(342,9),(343,9);

-- ── Collection product images (Unsplash robotics imagery) ────────
INSERT INTO product_images (imageID, productID, imageURL, isPrimary) VALUES
 (18, 300, 'https://images.unsplash.com/photo-1546776310-eef45dd6d63c?w=600', 1),
 (19, 301, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600', 1),
 (20, 302, 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=600', 1),
 (21, 303, 'https://images.unsplash.com/photo-1531746790731-6c087fecd65a?w=600', 1),
 (22, 304, 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600', 1),
 (23, 305, 'https://images.unsplash.com/photo-1563770660941-20978e870e26?w=600', 1),
 (24, 306, 'https://images.unsplash.com/photo-1580894908361-967195033215?w=600', 1),
 (25, 307, 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600', 1),
 (26, 308, 'https://images.unsplash.com/photo-1517365830460-955ce3ccd263?w=600', 1),
 (27, 309, 'https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=600', 1),
 (28, 310, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600', 1),

 (29, 311, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),
 (30, 312, 'https://images.unsplash.com/photo-1526570207772-784d36084510?w=600', 1),
 (31, 313, 'https://images.unsplash.com/photo-1521405924368-64c5b84bec60?w=600', 1),
 (32, 314, 'https://images.unsplash.com/photo-1558002038-1055907df827?w=600', 1),
 (33, 315, 'https://images.unsplash.com/photo-1589254065909-b7086229d08c?w=600', 1),
 (34, 316, 'https://images.unsplash.com/photo-1580894908361-967195033215?w=600', 1),
 (35, 317, 'https://images.unsplash.com/photo-1527689368864-3a821dbccc34?w=600', 1),
 (36, 318, 'https://images.unsplash.com/photo-1609592069181-cb4f2d6c1e9b?w=600', 1),
 (37, 319, 'https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?w=600', 1),
 (38, 320, 'https://images.unsplash.com/photo-1518186285589-2f7649de83e0?w=600', 1),
 (39, 321, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),

 (40, 322, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600', 1),
 (41, 323, 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=600', 1),
 (42, 324, 'https://images.unsplash.com/photo-1580894908361-967195033215?w=600', 1),
 (43, 325, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600', 1),
 (44, 326, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),
 (45, 327, 'https://images.unsplash.com/photo-1581090700227-1e37b190418e?w=600', 1),
 (46, 328, 'https://images.unsplash.com/photo-1526378800651-c32d170fe6f8?w=600', 1),
 (47, 329, 'https://images.unsplash.com/photo-1555617766-761fda5c83a7?w=600', 1),
 (48, 330, 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600', 1),
 (49, 331, 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=600', 1),
 (50, 332, 'https://images.unsplash.com/photo-1581092795360-fd1ca04f0952?w=600', 1),

 (51, 333, 'https://images.unsplash.com/photo-1565689157206-0fddef7589a2?w=600', 1),
 (52, 334, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600', 1),
 (53, 335, 'https://images.unsplash.com/photo-1565610222536-ef125c59da2e?w=600', 1),
 (54, 336, 'https://images.unsplash.com/photo-1581090700227-1e37b190418e?w=600', 1),
 (55, 337, 'https://images.unsplash.com/photo-1565608438257-fac3c27beb36?w=600', 1),
 (56, 338, 'https://images.unsplash.com/photo-1571855167162-30e827f39ba2?w=600', 1),
 (57, 339, 'https://images.unsplash.com/photo-1518186285589-2f7649de83e0?w=600', 1),
 (58, 340, 'https://images.unsplash.com/photo-1581090464777-f3220bbe1b8b?w=600', 1),
 (59, 341, 'https://images.unsplash.com/photo-1526378800651-c32d170fe6f8?w=600', 1),
 (60, 342, 'https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=600', 1),
 (61, 343, 'https://images.unsplash.com/photo-1580894908361-967195033215?w=600', 1);

-- ── Collection warehouse inventory (all at Central Warehouse = 1) ─
INSERT INTO warehouse_inventory (warehouseID, productID, quantityOnHand, minStockThreshold, reorderQuantity, lowStockFlag, unitCost, isActive) VALUES
 (1, 300,  6, 2,  4, 0, 3800.00, 1),
 (1, 301, 14, 3,  8, 0, 1500.00, 1),
 (1, 302,  9, 2,  5, 0, 2100.00, 1),
 (1, 303,  5, 2,  4, 0, 3000.00, 1),
 (1, 304,  8, 2,  5, 0, 2400.00, 1),
 (1, 305, 45, 8, 20, 0,  150.00, 1),
 (1, 306, 30, 5, 12, 0,   70.00, 1),
 (1, 307, 22, 5, 10, 0,  110.00, 1),
 (1, 308, 50, 0,  0, 0,   80.00, 1),
 (1, 309, 50, 0,  0, 0,  120.00, 1),
 (1, 310,  4, 2,  3, 0, 4800.00, 1),

 (1, 311,  7, 2,  4, 0, 2200.00, 1),
 (1, 312, 16, 4,  8, 0,  800.00, 1),
 (1, 313,  5, 2,  3, 0, 2800.00, 1),
 (1, 314, 10, 3,  5, 0, 1400.00, 1),
 (1, 315,  4, 2,  3, 0, 3800.00, 1),
 (1, 316, 42, 8, 20, 0,  110.00, 1),
 (1, 317, 28, 5, 12, 0,   55.00, 1),
 (1, 318, 24, 5, 10, 0,  140.00, 1),
 (1, 319, 99, 0,  0, 0,   30.00, 1),
 (1, 320, 99, 0,  0, 0,   90.00, 1),
 (1, 321,  3, 2,  2, 0, 5400.00, 1),

 (1, 322, 11, 3,  6, 0, 1400.00, 1),
 (1, 323, 17, 4,  8, 0,  900.00, 1),
 (1, 324, 38, 6, 15, 0,  110.00, 1),
 (1, 325, 45, 8, 20, 0,   85.00, 1),
 (1, 326,  8, 2,  4, 0, 2100.00, 1),
 (1, 327, 12, 3,  6, 0, 1100.00, 1),
 (1, 328, 26, 5, 10, 0,  190.00, 1),
 (1, 329, 35, 6, 14, 0,   50.00, 1),
 (1, 330, 29, 5, 12, 0,   80.00, 1),
 (1, 331, 99, 0,  0, 0,   25.00, 1),
 (1, 332, 99, 0,  0, 0,  350.00, 1),

 (1, 333,  3, 1,  2, 0, 9500.00, 1),
 (1, 334,  5, 2,  3, 0, 5800.00, 1),
 (1, 335,  6, 2,  3, 0, 7000.00, 1),
 (1, 336, 12, 3,  6, 0, 2600.00, 1),
 (1, 337,  7, 2,  4, 0, 4700.00, 1),
 (1, 338,  2, 1,  2, 0,11500.00, 1),
 (1, 339, 20, 5, 10, 0,  190.00, 1),
 (1, 340, 15, 4,  8, 0,  280.00, 1),
 (1, 341, 18, 4,  8, 0,  210.00, 1),
 (1, 342, 99, 0,  0, 0,  900.00, 1),
 (1, 343, 99, 0,  0, 0,  450.00, 1);

-- ================================================================
--  SMTP SETTINGS (single row, Super Admin editable)
-- ================================================================
INSERT INTO smtp_settings
 (settingID, host, port, username, password, fromAddress, fromName, useTls, isEnabled)
VALUES
 (1, 'smtp.gmail.com', 587, '', '', 'no-reply@raez.org.uk', 'RAEZ Support', 1, 0);

COMMIT;
