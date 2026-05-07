PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS product_collections (
    collectionID INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO categories (categoryName, description, isActive) VALUES
('Main Robot', 'Primary flagship robotics platforms', 1),
('Mini Robot', 'Compact robotics units and assistants', 1),
('Accessory', 'Add-ons, upgrades, and replacement components', 1),
('Service', 'Ongoing support, setup, and optimization services', 1);

DELETE FROM product_categories
WHERE productID IN (
    SELECT productID
    FROM products
    WHERE sku LIKE 'HTR-%'
       OR collectionID IN (1, 2, 3, 4)
       OR collection IN ('Apex Automata', 'Sentinel Force', 'NovaMind', 'TerraCore')
       OR name IN ('Red Dead', 'New Product', 'New Robot', 'Robot')
);

DELETE FROM product_images
WHERE productID IN (
    SELECT productID
    FROM products
    WHERE sku LIKE 'HTR-%'
       OR collectionID IN (1, 2, 3, 4)
       OR collection IN ('Apex Automata', 'Sentinel Force', 'NovaMind', 'TerraCore')
       OR name IN ('Red Dead', 'New Product', 'New Robot', 'Robot')
);

DELETE FROM product_validations
WHERE productID IN (
    SELECT productID
    FROM products
    WHERE sku LIKE 'HTR-%'
       OR collectionID IN (1, 2, 3, 4)
       OR collection IN ('Apex Automata', 'Sentinel Force', 'NovaMind', 'TerraCore')
       OR name IN ('Red Dead', 'New Product', 'New Robot', 'Robot')
);

DELETE FROM warehouse_inventory
WHERE productID IN (
    SELECT productID
    FROM products
    WHERE sku LIKE 'HTR-%'
       OR collectionID IN (1, 2, 3, 4)
       OR collection IN ('Apex Automata', 'Sentinel Force', 'NovaMind', 'TerraCore')
       OR name IN ('Red Dead', 'New Product', 'New Robot', 'Robot')
);

DELETE FROM products
WHERE (
    sku LIKE 'HTR-%'
    OR collectionID IN (1, 2, 3, 4)
    OR collection IN ('Apex Automata', 'Sentinel Force', 'NovaMind', 'TerraCore')
    OR name IN ('Red Dead', 'New Product', 'New Robot', 'Robot')
)
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.productID = products.productID);

DELETE FROM product_collections WHERE collectionID IN (1, 2, 3, 4);

INSERT INTO product_collections (collectionID, name, slug, description) VALUES
(1, 'The Apex Series', 'the-apex-series', 'Flagship autonomous orchestrators and precision companions'),
(2, 'The Ledger Series', 'the-ledger-series', 'Enterprise inventory and audit robotics systems'),
(3, 'The Velocity Series', 'the-velocity-series', 'High-agility response robotics for dynamic terrain'),
(4, 'The Sentinel Series', 'the-sentinel-series', 'Defense-first monitoring and perimeter automation');

INSERT INTO products (sku, name, description, price, unitCost, status, categoryID, collection, collectionID)
SELECT 'HTR-001', 'Aetherion Prime', 'Our flagship bipedal orchestrator. Powered by an advanced agentic AI core, it simulates millions of real-world variables per second using internal world models, allowing it to execute complex physical tasks without human intervention.', 125000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-002', 'Aura-Scout V1', 'An aerial companion drone that maps spatial environments in real-time.', 8500.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-003', 'Nexus-Node', 'A stationary, deployable micro-server that acts as a local bridge for fleet synchronization.', 4200.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-004', 'Pulse-Crawler', 'A micro-terrain inspector for tight spaces to scrape environmental data.', 5000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-005', 'Quantum Lidar Array', 'Expands spatial perception range by 400%.', 12000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-006', 'Kinetic Recharge Dock', 'Inductive charging station via ambient kinetic energy transfer.', 3500.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-007', 'Neural-Link Expansion Drive', 'Modular compute stick boosting parallel processing.', 6000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-008', 'Titanium Alloy Exoskeleton', 'Upgraded armor plating for hazardous environments.', 15000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-009', 'Omni-Directional Articulation Joints', 'Replacement servos for 360-degree limb rotation.', 4800.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-010', 'Continuous World-Model Training', 'Weekly neural-net updates for the logic engine.', 2000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-011', 'Predictive Maintenance Care Plan', 'Automated remote diagnostics and part dispatch.', 1500.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-012', 'Fleet Synchronization Setup', 'Expert remote calibration for hive-mind workflow.', 5000.00, 0.00, 'active', c.categoryID, 'The Apex Series', 1 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-013', 'Ledger-Bot X9', 'The ultimate enterprise auditor. A heavily armored automation chassis built for high-frequency inventory tracking and bridging physical supply chains directly into e-commerce reporting modules.', 85000.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-014', 'Quant-Mite', 'Swarm-capable micro-auditor for RFID scanning.', 3200.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-015', 'Ticker-Drone', 'Rapid-response airborne courier for secure encrypted data drives.', 5500.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-016', 'DataVault Crawler', 'Fortified terrestrial unit for transporting offline backup servers.', 9000.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-017', 'Encrypted Comms Uplink', 'Secure communication uplink module with hardened channel encryption.', 2500.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-018', 'High-Fidelity Biometric Scanner', 'Precision biometric scanning attachment for access validation.', 1800.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-019', 'Extended 72-Hour Battery Cell', 'Long-endurance power cell for uninterrupted auditing cycles.', 4000.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-020', 'Thermal Receipt Module', 'Compact thermal receipt and transaction evidence printer.', 800.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-021', 'Swappable Payload Trays', 'Modular tray pack for secure payload rotation.', 1200.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-022', 'ERP API Integration Consulting', 'Expert integration of robotics telemetry with enterprise ERP platforms.', 10000.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-023', 'Quarterly Compliance Audit', 'Scheduled audit package for operational and data compliance readiness.', 3000.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-024', '24/7 Automated Remote Support', 'Always-on support and autonomous issue triage services.', 2500.00, 0.00, 'active', c.categoryID, 'The Ledger Series', 2 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-025', 'Veloce-Mach 1', 'A hyper-agile quadruped optimized for rapid response. Calculates extreme trajectories and navigates complex terrain with motorsport-level precision using differentiable physics.', 110000.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-026', 'Aero-Slip', 'Compact mobility unit built for fast-response reconnaissance maneuvers.', 4000.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-027', 'G-Force Tracker', 'Telemetry scout robot designed for acceleration and force profiling.', 2200.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-028', 'Apex-Rover', 'All-surface rapid rover for extreme route validation.', 6500.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-029', 'Carbon Fiber Chassis Upgrade', 'Weight-optimized chassis enhancement for speed and rigidity.', 18000.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-030', 'High-Torque Servo Kit', 'High-output servo replacement set for aggressive load handling.', 5000.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-031', 'Dynamic Aero-Spoilers', 'Adaptive airflow kit for improved traction and cornering stability.', 3500.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-032', 'Telemetry Blackbox', 'Secure mission recorder for high-frequency performance logs.', 2800.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-033', 'Micro-Traction Treads', 'Terrain-specific micro-tread module for slip-resistant mobility.', 1500.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-034', 'High-Frequency Telemetry Analytics', 'Continuous analytics package for aggressive performance tuning.', 1800.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-035', 'Custom Dynamic Calibration', 'Specialist calibration engagement for dynamic handling profiles.', 4500.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-036', 'Priority Part Replacement', 'Expedited replacement coverage for high-wear motion components.', 1200.00, 0.00, 'active', c.categoryID, 'The Velocity Series', 3 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-037', 'Aegis-Warden', 'An imposing, heavily shielded automaton designed for perimeter defense and continuous environmental monitoring in extreme conditions.', 95000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-038', 'Sonar-Bat', 'Acoustic mapping mini unit for blind-zone perimeter analysis.', 3800.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-039', 'Thermo-Gnat', 'Thermal anomaly mini scout for heat signature surveillance.', 4200.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-040', 'Volt-Tick', 'Micro-electric probe unit for grid stability and line diagnostics.', 2000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-041', 'Reinforced Tungsten Plating', 'Heavy-duty armor reinforcement for impact-critical deployments.', 12000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-042', 'Infrared Lenses', 'Infrared optics expansion kit for thermal visibility.', 3000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-043', 'EMP Shielding Mesh', 'Electromagnetic pulse protection mesh for mission continuity.', 8500.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-044', 'High-Decibel Siren Module', 'Integrated deterrence siren with programmable threat profiles.', 1100.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-045', 'Chemical Analyzer Probe', 'Environmental chemistry probe for hazardous condition detection.', 6000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Accessory'
UNION ALL SELECT 'HTR-046', 'Perimeter Mapping & Optimization', 'Professional threat-grid mapping and perimeter optimization service.', 5500.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-047', 'Threat Simulation Drills', 'Scenario-driven simulation drills for defensive readiness.', 4000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-048', 'Bi-Annual Hardware Decontamination', 'Twice-yearly decontamination and resilience certification service.', 2000.00, 0.00, 'active', c.categoryID, 'The Sentinel Series', 4 FROM categories c WHERE c.categoryName = 'Service'
UNION ALL SELECT 'HTR-049', 'Cipher-Sentinel', 'Cybersecurity defense automaton for localized network monitoring.', 75000.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-050', 'Echo-Medic', 'Mobile triage and biometric medical record syncing unit.', 88000.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-051', 'Solara-Harvest', 'Agricultural stochastic modeling and crop health automaton.', 62000.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Main Robot'
UNION ALL SELECT 'HTR-052', 'Scrap-Byte', 'Salvage-focused micro robot for workshop material sorting.', 1500.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-053', 'Lens-Flare', 'Optics calibration mini drone for visual system alignment.', 2800.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-054', 'Synth-Weaver', 'Micro-fabrication mini unit for polymer weave prototyping.', 4500.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-055', 'Echo-Ping', 'Ultra-light beacon mini robot for acoustic relay coverage.', 900.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Mini Robot'
UNION ALL SELECT 'HTR-056', 'Aero-Breeze', 'Compact airflow mini companion for ventilation diagnostics.', 1200.00, 0.00, 'active', c.categoryID, NULL, NULL FROM categories c WHERE c.categoryName = 'Mini Robot';

INSERT OR IGNORE INTO product_categories (productID, categoryID)
SELECT p.productID, p.categoryID
FROM products p
WHERE p.sku LIKE 'HTR-%'
  AND p.categoryID IS NOT NULL;

-- Seed warehouse inventory for every robotics storefront product so the
-- Add to Cart / Buy Now flows are always testable (stock >= 50).
INSERT OR IGNORE INTO warehouse_inventory
    (warehouseID, productID, quantityOnHand, minStockThreshold, reorderQuantity, lowStockFlag, unitCost, isActive)
SELECT 1, p.productID, 75, 10, 25, 0, p.unitCost, 1
FROM products p
WHERE p.sku LIKE 'HTR-%';

-- Top-up: if an earlier boot inserted a row with a stale low quantity,
-- push Apex and Ledger series to at least 50 so the demo never shows
-- "Out of Stock" for the flagship collections.
UPDATE warehouse_inventory
SET quantityOnHand = 75
WHERE quantityOnHand < 50
  AND productID IN (
      SELECT productID FROM products WHERE sku LIKE 'HTR-%'
  );

COMMIT;
