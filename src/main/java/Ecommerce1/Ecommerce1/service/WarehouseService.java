package Ecommerce1.Ecommerce1.service;

import Ecommerce1.Ecommerce1.Warehouse_DatabaseConnection;
import Ecommerce1.Ecommerce1.model.Warehouse_LowStockRow;
import Ecommerce1.Ecommerce1.model.Warehouse_Product;
import Ecommerce1.Ecommerce1.model.Warehouse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarehouseService {

    // ====================================================
    // DATABASE LOAD
    // ====================================================

    public ObservableList<Warehouse> loadWarehousesFromDb() {
        ObservableList<Warehouse> result = FXCollections.observableArrayList();

        // CHANGED: Warehouse → warehouse_warehouses
        String warehouseSql = "SELECT warehouseID, warehouseName, location, capacityLimit, warehouseCode FROM warehouse_warehouses";

        // CHANGED: InventoryRecord → warehouse_inventory, Product → products
        String inventorySql = "SELECT p.productID, p.name, ir.quantityOnHand, " +
                              "ir.minStockThreshold, ir.reorderQuantity, ir.lastRestockDate " +
                              "FROM warehouse_inventory ir " +
                              "JOIN products p ON ir.productID = p.productID " +
                              "WHERE ir.warehouseID = ?";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection();
             PreparedStatement warehouseStmt = conn.prepareStatement(warehouseSql);
             ResultSet warehouseRs = warehouseStmt.executeQuery()) {

            while (warehouseRs.next()) {
                int    dbId     = warehouseRs.getInt("warehouseID");
                String whId     = warehouseRs.getString("warehouseCode") != null
                        ? warehouseRs.getString("warehouseCode")
                        : "WH-" + String.format("%03d", dbId);
                String name     = warehouseRs.getString("warehouseName");
                String location = warehouseRs.getString("location");
                int    capacity = warehouseRs.getInt("capacityLimit");

                Warehouse w = new Warehouse(String.valueOf(dbId), whId, name, location, "", capacity);

                try (PreparedStatement invStmt = conn.prepareStatement(inventorySql)) {
                    invStmt.setInt(1, dbId);
                    ResultSet invRs = invStmt.executeQuery();
                    while (invRs.next()) {
                        Warehouse_Product p = new Warehouse_Product(
                                String.valueOf(invRs.getInt("productID")),
                                invRs.getString("name"),
                                "PRD-" + invRs.getInt("productID"),
                                invRs.getInt("quantityOnHand"),
                                invRs.getInt("minStockThreshold"),
                                invRs.getInt("reorderQuantity"),
                                invRs.getString("lastRestockDate") != null
                                        ? invRs.getString("lastRestockDate") : ""
                        );
                        w.getProducts().add(p);
                    }
                }
                result.add(w);
            }

        } catch (SQLException e) {
            System.err.println("WarehouseService.loadWarehousesFromDb error: " + e.getMessage());
        }

        return result;
    }

    public ObservableList<UserProductRow> loadUserProductsFromDb() {
        ObservableList<UserProductRow> result = FXCollections.observableArrayList();

        // CHANGED: InventoryRecord → warehouse_inventory, Product → products, Warehouse → warehouse_warehouses
        String sql = "SELECT p.name, p.productID, ir.quantityOnHand, ir.minStockThreshold, " +
                     "w.warehouseName, w.location, COALESCE(w.productLine, 'General') AS productLine " +
                     "FROM warehouse_inventory ir " +
                     "JOIN products p ON ir.productID = p.productID " +
                     "JOIN warehouse_warehouses w ON ir.warehouseID = w.warehouseID";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name      = rs.getString("name");
                String sku       = "PRD-" + rs.getInt("productID");
                int    qty       = rs.getInt("quantityOnHand");
                int    min       = rs.getInt("minStockThreshold");
                String warehouse = rs.getString("warehouseName") + " - " + rs.getString("location");
                String category  = rs.getString("productLine");
                result.add(new UserProductRow(name, sku, qty, min, warehouse, category));
            }

        } catch (SQLException e) {
            System.err.println("WarehouseService.loadUserProductsFromDb error: " + e.getMessage());
        }

        return result;
    }

    // ====================================================
    // DATABASE SAVE — WAREHOUSE
    // ====================================================

    public int saveWarehouseToDb(String name, String location, String contactEmail, int capacity) {
        // CHANGED: Warehouse → warehouse_warehouses
        String sql = "INSERT INTO warehouse_warehouses (warehouseName, location, contactEmail, capacityLimit, warehouseCode) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Warehouse_DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, location);
            stmt.setString(3, contactEmail);
            stmt.setInt(4, capacity);
            stmt.setString(5, "WH-TEMP");
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                String whCode = getNextWarehouseCode(conn);
                // CHANGED: Warehouse → warehouse_warehouses
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE warehouse_warehouses SET warehouseCode=? WHERE warehouseID=?")) {
                    update.setString(1, whCode);
                    update.setInt(2, newId);
                    update.executeUpdate();
                }
                return newId;
            }

        } catch (SQLException e) {
            System.err.println("WarehouseService.saveWarehouseToDb error: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateWarehouseInDb(int warehouseDbId, String name, String location,
                                        String contactEmail, int capacity) {
        // CHANGED: Warehouse → warehouse_warehouses
        String sql = "UPDATE warehouse_warehouses SET warehouseName=?, location=?, contactEmail=?, " +
                     "capacityLimit=? WHERE warehouseID=?";
        try (Connection conn = Warehouse_DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, location);
            stmt.setString(3, contactEmail);
            stmt.setInt(4, capacity);
            stmt.setInt(5, warehouseDbId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("WarehouseService.updateWarehouseInDb error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteWarehouseFromDb(int warehouseDbId) {
        // CHANGED: Warehouse → warehouse_warehouses
        String sql = "DELETE FROM warehouse_warehouses WHERE warehouseID=?";
        try (Connection conn = Warehouse_DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, warehouseDbId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("WarehouseService.deleteWarehouseFromDb error: " + e.getMessage());
            return false;
        }
    }

    // ====================================================
    // DATABASE SAVE — PRODUCT / INVENTORY
    // ====================================================

    public int saveProductToDb(int warehouseDbId, String name, int quantity,
                                int minThreshold, int reorderLevel,
                                String lastRestock, double price, int performedByUserID) {
        // CHANGED: Product → products, removed stock column (now in warehouse_inventory only)
        String insertProduct   = "INSERT INTO products (name, price, status) VALUES (?, ?, 'active')";
        // CHANGED: InventoryRecord → warehouse_inventory
        String insertInventory = "INSERT INTO warehouse_inventory (warehouseID, productID, quantityOnHand, " +
                                 "minStockThreshold, reorderQuantity, lastRestockDate) VALUES (?,?,?,?,?,?)";
        // NEW: log stock movement with performedByUserID
        String insertMovement  = "INSERT INTO warehouse_stock_movements (inventoryID, toWarehouseID, " +
                                 "quantityChanged, movementType, movementDate, performedByUserID) " +
                                 "VALUES (?, ?, ?, 'IN', datetime('now'), ?)";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            int productId;
            try (PreparedStatement ps = conn.prepareStatement(insertProduct, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return -1; }
                productId = keys.getInt(1);
            }

            int inventoryId;
            try (PreparedStatement ps = conn.prepareStatement(insertInventory, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, warehouseDbId);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                ps.setInt(4, minThreshold);
                ps.setInt(5, reorderLevel);
                ps.setString(6, lastRestock.isEmpty() ? null : lastRestock);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return -1; }
                inventoryId = keys.getInt(1);
            }

            // NEW: log the stock movement
            try (PreparedStatement ps = conn.prepareStatement(insertMovement)) {
                ps.setInt(1, inventoryId);
                ps.setInt(2, warehouseDbId);
                ps.setInt(3, quantity);
                ps.setInt(4, performedByUserID);
                ps.executeUpdate();
            }

            conn.commit();
            return productId;

        } catch (SQLException e) {
            System.err.println("WarehouseService.saveProductToDb error: " + e.getMessage());
            return -1;
        }
    }

    public boolean updateProductInDb(int productDbId, int warehouseDbId, String name,
                                      int quantity, int minThreshold,
                                      int reorderLevel, String lastRestock,
                                      int performedByUserID) {
        // CHANGED: Product → products, removed stock column
        String updateProduct   = "UPDATE products SET name=? WHERE productID=?";
        // CHANGED: InventoryRecord → warehouse_inventory
        String updateInventory = "UPDATE warehouse_inventory SET quantityOnHand=?, minStockThreshold=?, " +
                                 "reorderQuantity=?, lastRestockDate=? " +
                                 "WHERE warehouseID=? AND productID=?";
        // NEW: log stock movement with performedByUserID
        String insertMovement  = "INSERT INTO warehouse_stock_movements (inventoryID, toWarehouseID, " +
                                 "quantityChanged, movementType, movementDate, performedByUserID) " +
                                 "VALUES ((SELECT inventoryID FROM warehouse_inventory WHERE productID=? AND warehouseID=?), " +
                                 "?, ?, 'ADJUSTMENT', datetime('now'), ?)";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(updateProduct)) {
                ps.setString(1, name);
                ps.setInt(2, productDbId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(updateInventory)) {
                ps.setInt(1, quantity);
                ps.setInt(2, minThreshold);
                ps.setInt(3, reorderLevel);
                ps.setString(4, lastRestock.isEmpty() ? null : lastRestock);
                ps.setInt(5, warehouseDbId);
                ps.setInt(6, productDbId);
                ps.executeUpdate();
            }

            // NEW: log the movement
            try (PreparedStatement ps = conn.prepareStatement(insertMovement)) {
                ps.setInt(1, productDbId);
                ps.setInt(2, warehouseDbId);
                ps.setInt(3, warehouseDbId);
                ps.setInt(4, quantity);
                ps.setInt(5, performedByUserID);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("WarehouseService.updateProductInDb error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProductFromDb(int productDbId, int warehouseDbId) {
        // CHANGED: InventoryRecord → warehouse_inventory, Product → products
        String deleteInventory = "DELETE FROM warehouse_inventory WHERE productID=? AND warehouseID=?";
        String deleteProduct   = "DELETE FROM products WHERE productID=?";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(deleteInventory)) {
                ps.setInt(1, productDbId);
                ps.setInt(2, warehouseDbId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteProduct)) {
                ps.setInt(1, productDbId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("WarehouseService.deleteProductFromDb error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStockTransferInDb(int fromProductDbId, int fromWarehouseDbId,
                                            int toProductDbId, int toWarehouseDbId,
                                            int quantity, int performedByUserID) {
        // CHANGED: InventoryRecord → warehouse_inventory
        String subtract = "UPDATE warehouse_inventory SET quantityOnHand = quantityOnHand - ? " +
                          "WHERE productID=? AND warehouseID=?";
        String add      = "UPDATE warehouse_inventory SET quantityOnHand = quantityOnHand + ? " +
                          "WHERE productID=? AND warehouseID=?";
        String insert   = "INSERT INTO warehouse_inventory (warehouseID, productID, quantityOnHand) VALUES (?,?,?)";
        // NEW: log stock movement with performedByUserID
        // CHANGED: StockMovement → warehouse_stock_movements, added performedByUserID
        String movement = "INSERT INTO warehouse_stock_movements (inventoryID, fromWarehouseID, toWarehouseID, " +
                          "quantityChanged, movementType, movementDate, performedByUserID) " +
                          "VALUES ((SELECT inventoryID FROM warehouse_inventory WHERE productID=? AND warehouseID=?), " +
                          "?, ?, ?, 'TRANSFER', datetime('now'), ?)";

        try (Connection conn = Warehouse_DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(subtract)) {
                ps.setInt(1, quantity);
                ps.setInt(2, fromProductDbId);
                ps.setInt(3, fromWarehouseDbId);
                ps.executeUpdate();
            }

            // CHANGED: InventoryRecord → warehouse_inventory
            String check = "SELECT COUNT(*) FROM warehouse_inventory WHERE productID=? AND warehouseID=?";
            int count = 0;
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, toProductDbId);
                ps.setInt(2, toWarehouseDbId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) count = rs.getInt(1);
            }

            if (count > 0) {
                try (PreparedStatement ps = conn.prepareStatement(add)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, toProductDbId);
                    ps.setInt(3, toWarehouseDbId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, toWarehouseDbId);
                    ps.setInt(2, toProductDbId);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
            }

            // NEW: log the transfer movement
            try (PreparedStatement ps = conn.prepareStatement(movement)) {
                ps.setInt(1, toProductDbId);
                ps.setInt(2, toWarehouseDbId);
                ps.setInt(3, fromWarehouseDbId);
                ps.setInt(4, toWarehouseDbId);
                ps.setInt(5, quantity);
                ps.setInt(6, performedByUserID);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("WarehouseService.updateStockTransferInDb error: " + e.getMessage());
            return false;
        }
    }

    // ====================================================
    // BUSINESS LOGIC
    // ====================================================

    public TransferResult transferStock(Warehouse from, Warehouse to,
                                        Warehouse_Product warehouse_Product, int quantity) {
        if (from == null || to == null || warehouse_Product == null)
            return TransferResult.fail("Invalid transfer parameters.");
        if (from == to)
            return TransferResult.fail("Cannot transfer to the same warehouse.");
        if (quantity <= 0)
            return TransferResult.fail("Quantity must be a positive number.");
        if (quantity > warehouse_Product.getQuantity())
            return TransferResult.fail("Insufficient stock. Available: " + warehouse_Product.getQuantity() + " units.");
        if (to.getAvailableSpace() < quantity)
            return TransferResult.fail("Transfer would exceed destination warehouse capacity. " +
                    "Available space: " + to.getAvailableSpace() + " units.");

        warehouse_Product.setQuantity(warehouse_Product.getQuantity() - quantity);

        Warehouse_Product existing = to.getProducts().stream()
                .filter(p -> p.getSku().equalsIgnoreCase(warehouse_Product.getSku()))
                .findFirst().orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            Warehouse_Product copy = new Warehouse_Product("p" + System.currentTimeMillis(),
                    warehouse_Product.getName(), warehouse_Product.getSku(), quantity,
                    warehouse_Product.getMinThreshold(), warehouse_Product.getReorderLevel(), warehouse_Product.getLastRestock());
            to.getProducts().add(copy);
        }

        return TransferResult.success();
    }

    public AddProductResult addProduct(Warehouse warehouse, String name, String sku,
                                       int quantity, int minThreshold,
                                       int reorderLevel, String lastRestock) {
        if (warehouse == null)
            return AddProductResult.fail("Please select a warehouse.");
        if (warehouse.hasProductWithSku(sku))
            return AddProductResult.fail("A product with this SKU already exists in the selected warehouse.");
        if (warehouse.getAvailableSpace() < quantity)
            return AddProductResult.fail("Adding this product would exceed warehouse capacity. " +
                    "Available space: " + warehouse.getAvailableSpace() + " units.");

        Warehouse_Product newProduct = new Warehouse_Product("p" + System.currentTimeMillis(),
                name, sku, quantity, minThreshold, reorderLevel, lastRestock);
        warehouse.getProducts().add(newProduct);
        return AddProductResult.success(newProduct);
    }

    public UpdateProductResult updateProduct(Warehouse warehouse, Warehouse_Product original,
                                             String name, String sku, int quantity,
                                             int minThreshold, int reorderLevel) {
        if (!sku.equalsIgnoreCase(original.getSku()) && warehouse.hasProductWithSku(sku))
            return UpdateProductResult.fail("A product with this SKU already exists in this warehouse.");

        int capacityDiff = quantity - original.getQuantity();
        if (warehouse.getCurrentStock() + capacityDiff > warehouse.getMaxCapacity()) {
            int available = warehouse.getMaxCapacity() - warehouse.getCurrentStock() + original.getQuantity();
            return UpdateProductResult.fail("This change would exceed warehouse capacity. " +
                    "Available space: " + available + " units.");
        }

        Warehouse_Product updated = new Warehouse_Product(original.getId(), name, sku, quantity,
                minThreshold, reorderLevel, original.getLastRestock());
        return UpdateProductResult.success(updated);
    }

    public List<Warehouse_LowStockRow> getLowStockRows(ObservableList<Warehouse> warehouses) {
        List<Warehouse_LowStockRow> rows = new ArrayList<>();
        for (Warehouse w : warehouses)
            for (Warehouse_Product p : w.getProducts())
                if (p.isLowStock())
                    rows.add(new Warehouse_LowStockRow(p.getName(), p.getSku(), p.getQuantity(), w.getLocation()));
        return rows;
    }

    public int getTotalStock(ObservableList<Warehouse> warehouses) {
        return warehouses.stream().mapToInt(Warehouse::getCurrentStock).sum();
    }

    public int getCapacityUsagePercent(ObservableList<Warehouse> warehouses) {
        int totalStock = getTotalStock(warehouses);
        int totalMax   = warehouses.stream().mapToInt(Warehouse::getMaxCapacity).sum();
        return totalMax == 0 ? 0 : Math.round((totalStock * 100f) / totalMax);
    }

    // ====================================================
    // WAREHOUSE CODE HELPER
    // ====================================================

    private String getNextWarehouseCode(Connection conn) throws SQLException {
        // CHANGED: Warehouse → warehouse_warehouses
        String sql = "SELECT warehouseCode FROM warehouse_warehouses ORDER BY warehouseCode";
        int next = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("warehouseCode");
                if (code != null && code.startsWith("WH-")) {
                    try {
                        int num = Integer.parseInt(code.substring(3));
                        if (num == next) next++;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return "WH-" + String.format("%03d", next);
    }

    // ====================================================
    // USER PRODUCT ROW MODEL
    // ====================================================

    public static class UserProductRow {
        private final String name;
        private final String sku;
        private final int    quantity;
        private final int    minThreshold;
        private final String warehouse;
        private final String category;

        public UserProductRow(String name, String sku, int quantity,
                               int minThreshold, String warehouse, String category) {
            this.name         = name;
            this.sku          = sku;
            this.quantity     = quantity;
            this.minThreshold = minThreshold;
            this.warehouse    = warehouse;
            this.category     = category;
        }

        public String getName()         { return name; }
        public String getSku()          { return sku; }
        public int    getQuantity()     { return quantity; }
        public int    getMinThreshold() { return minThreshold; }
        public String getWarehouse()    { return warehouse; }
        public String getCategory()     { return category; }
        public String getStatus()       { return quantity < minThreshold ? "Low Stock" : "In Stock"; }
    }

    // ====================================================
    // RESULT CLASSES
    // ====================================================

    public static class TransferResult {
        private final boolean success;
        private final String  message;
        private TransferResult(boolean success, String message) { this.success = success; this.message = message; }
        public static TransferResult success()            { return new TransferResult(true, null); }
        public static TransferResult fail(String message) { return new TransferResult(false, message); }
        public boolean isSuccess()  { return success; }
        public String  getMessage() { return message; }
    }

    public static class AddProductResult {
        private final boolean success;
        private final String  message;
        private final Warehouse_Product warehouse_Product;
        private AddProductResult(boolean success, String message, Warehouse_Product warehouse_Product) {
            this.success = success; this.message = message; this.warehouse_Product = warehouse_Product;
        }
        public static AddProductResult success(Warehouse_Product p)   { return new AddProductResult(true, null, p); }
        public static AddProductResult fail(String message) { return new AddProductResult(false, message, null); }
        public boolean isSuccess()  { return success; }
        public String  getMessage() { return message; }
        public Warehouse_Product getProduct() { return warehouse_Product; }
    }

    public static class UpdateProductResult {
        private final boolean success;
        private final String  message;
        private final Warehouse_Product warehouse_Product;
        private UpdateProductResult(boolean success, String message, Warehouse_Product warehouse_Product) {
            this.success = success; this.message = message; this.warehouse_Product = warehouse_Product;
        }
        public static UpdateProductResult success(Warehouse_Product p)   { return new UpdateProductResult(true, null, p); }
        public static UpdateProductResult fail(String message) { return new UpdateProductResult(false, message, null); }
        public boolean isSuccess()  { return success; }
        public String  getMessage() { return message; }
        public Warehouse_Product getProduct() { return warehouse_Product; }
    }
}