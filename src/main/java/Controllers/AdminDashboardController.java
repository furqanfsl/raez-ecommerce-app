package Controllers;

import com.reaz.model.Product;
import com.reaz.service.ProductService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {

    @FXML private Label      statTotal;
    @FXML private Label      statActive;
    @FXML private Label      statInactive;
    @FXML private Label      statOutOfStock;
    @FXML private Label      showingLabel;
    @FXML private MenuButton sortMenuBtn;

    @FXML private ProductFiltersController productFiltersController;
    @FXML private ProductTableController   productTableController;

    private final ProductService service = new ProductService();
    private List<Product> allProducts;
    private String currentSort    = "updatedAt-desc";
    private String filterSearch   = "";
    private String filterCategory = "";
    private String filterMinPrice = "";
    private String filterMaxPrice = "";
    private String filterStatus   = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadFromDb();
        if (productFiltersController != null) {
            productFiltersController.setOnFilterChange(f -> {
                filterSearch   = f.search;
                filterCategory = f.category;
                filterMinPrice = f.minPrice;
                filterMaxPrice = f.maxPrice;
                filterStatus   = f.status;
                refreshTable();
            });
        }
    }

    private void loadFromDb() {
        allProducts = service.getAll();
        refreshStats();
        refreshTable();
    }

    private void refreshStats() {
        if (statTotal != null)
            statTotal.setText(String.valueOf(allProducts.size()));
        if (statActive != null)
            statActive.setText(String.valueOf(
                allProducts.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.status)).count()));
        if (statInactive != null)
            statInactive.setText(String.valueOf(
                allProducts.stream().filter(p -> "INACTIVE".equalsIgnoreCase(p.status)).count()));
        if (statOutOfStock != null)
            statOutOfStock.setText(String.valueOf(
                allProducts.stream().filter(p -> p.stock == 0).count()));
    }

    private List<Product> getFiltered() {
        return allProducts.stream()
            .filter(p -> filterSearch.isEmpty() ||
                p.name.toLowerCase().contains(filterSearch.toLowerCase()) ||
                (p.description != null && p.description.toLowerCase()
                    .contains(filterSearch.toLowerCase())))
            .filter(p -> filterCategory.isEmpty() ||
                p.getCategoryNames().contains(filterCategory))
            .filter(p -> {
                if (filterMinPrice.isEmpty()) return true;
                try { return p.price >= Double.parseDouble(filterMinPrice); }
                catch (NumberFormatException e) { return true; }
            })
            .filter(p -> {
                if (filterMaxPrice.isEmpty()) return true;
                try { return p.price <= Double.parseDouble(filterMaxPrice); }
                catch (NumberFormatException e) { return true; }
            })
            .filter(p -> filterStatus.isEmpty() || p.status.equalsIgnoreCase(filterStatus))
            .collect(Collectors.toList());
    }

    private void refreshTable() {
        List<Product> filtered = getFiltered();

        Comparator<Product> cmp = switch (currentSort) {
            case "price-asc"  -> Comparator.comparingDouble(p -> p.price);
            case "price-desc" -> Comparator.<Product, Double>comparing(p -> p.price).reversed();
            case "name-asc"   -> Comparator.comparing(p -> p.name);
            case "name-desc"  -> Comparator.<Product, String>comparing(p -> p.name).reversed();
            case "stock-asc"  -> Comparator.comparingInt(p -> p.stock);
            default           -> Comparator.<Product, String>comparing(
                                    p -> p.updatedAt != null ? p.updatedAt : "").reversed();
        };
        filtered.sort(cmp);

        if (showingLabel != null)
            showingLabel.setText("Showing " + filtered.size() + " of " + allProducts.size());

        if (productTableController != null) {
            productTableController.setProducts(filtered,
                this::handleEdit,
                this::handleDelete,
                this::handleToggleStatus);
        }
    }

    // ── Sort handlers ─────────────────────────────────────────────────────

    @FXML private void handleSortUpdated()   { currentSort = "updatedAt-desc"; refreshTable(); }
    @FXML private void handleSortPriceAsc()  { currentSort = "price-asc";      refreshTable(); }
    @FXML private void handleSortPriceDesc() { currentSort = "price-desc";     refreshTable(); }
    @FXML private void handleSortNameAsc()   { currentSort = "name-asc";       refreshTable(); }
    @FXML private void handleSortNameDesc()  { currentSort = "name-desc";      refreshTable(); }
    @FXML private void handleSortStockAsc()  { currentSort = "stock-asc";      refreshTable(); }

    // ── CRUD handlers ─────────────────────────────────────────────────────

    @FXML private void handleAddProduct() {
        ProductFormDialogLauncher.show(null, product -> {
            try {
                service.add(product,
                    product.categories.stream().map(c -> c.name).collect(Collectors.toList()),
                    product.images.stream().map(i -> i.imagePath).collect(Collectors.toList()));
                loadFromDb();
            } catch (Exception e) {
                System.err.println("Add failed: " + e.getMessage());
            }
        });
    }

    private void handleEdit(Product product) {
        ProductFormDialogLauncher.show(product, updated -> {
            try {
                updated.id = product.id;
                service.update(updated,
                    updated.categories.stream().map(c -> c.name).collect(Collectors.toList()),
                    updated.images.stream().map(i -> i.imagePath).collect(Collectors.toList()));
                loadFromDb();
            } catch (Exception e) {
                System.err.println("Update failed: " + e.getMessage());
            }
        });
    }

    private void handleDelete(int id) {
        String name = allProducts.stream().filter(p -> p.id == id)
            .findFirst().map(p -> p.name).orElse("");
        DeleteConfirmDialogLauncher.show(name, () -> {
            service.delete(id);
            loadFromDb();
        });
    }

    private void handleToggleStatus(int id, String currentStatus) {
        service.toggleStatus(id, currentStatus);
        loadFromDb();
    }

    // ── Export / Import ───────────────────────────────────────────────────

    @FXML private void handleExport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Export Products");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
        fc.setInitialFileName("RAEZ-products.json");
        java.io.File file = fc.showSaveDialog(sortMenuBtn.getScene().getWindow());
        if (file == null) return;
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < allProducts.size(); i++) {
            Product p = allProducts.get(i);
            sb.append("  {\"id\":").append(p.id)
              .append(",\"name\":\"").append(p.name)
              .append("\",\"price\":").append(p.price)
              .append(",\"stock\":").append(p.stock)
              .append(",\"status\":\"").append(p.status).append("\"}");
            if (i < allProducts.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        try (java.io.FileWriter fw = new java.io.FileWriter(file)) {
            fw.write(sb.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleImport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Import Products");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
        fc.showOpenDialog(sortMenuBtn.getScene().getWindow());
    }

    // ── Navigation ────────────────────────────────────────────────────────

    // FIX: removed (BorderPane) cast — root is a VBox, use setRoot() instead
    @FXML private void handleBackToHome() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/homepage.fxml"));
            sortMenuBtn.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("Back to home failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}