package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class SideBarController implements Initializable {

    // Toggle arrows
    @FXML private Label categoryArrow;
    @FXML private Label priceArrow;
    @FXML private Label featuresArrow;
    @FXML private Label brandArrow;

    // Panels
    @FXML private VBox categoryPanel;
    @FXML private VBox pricePanel;
    @FXML private VBox featuresPanel;
    @FXML private VBox brandPanel;

    // Category checkboxes
    @FXML private CheckBox catAllRobots;
    @FXML private CheckBox catHomeAssistants;
    @FXML private CheckBox catSecurityBots;
    @FXML private CheckBox catEducational;
    @FXML private CheckBox catCompanions;
    @FXML private CheckBox catIndustrial;

    // Price checkboxes
    @FXML private CheckBox priceUnder200;
    @FXML private CheckBox price200to400;
    @FXML private CheckBox price400to600;
    @FXML private CheckBox priceOver600;

    // Search field
    @FXML private TextField searchField;

    // Reference to product list — set by homepage controller or via lookup
    private ProductListController productListController;

    public void setProductListController(ProductListController c) {
        this.productListController = c;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    // ── Toggle panels ──────────────────────────────────────
    @FXML private void toggleCategory() { toggle(categoryPanel, categoryArrow); }
    @FXML private void togglePrice()    { toggle(pricePanel,    priceArrow);    }
    @FXML private void toggleFeatures() { toggle(featuresPanel, featuresArrow); }
    @FXML private void toggleBrand()    { toggle(brandPanel,    brandArrow);    }

    private void toggle(VBox panel, Label arrow) {
        boolean visible = !panel.isVisible();
        panel.setVisible(visible);
        panel.setManaged(visible);
        arrow.setText(visible ? "∧" : "∨");
    }

    // ── Apply filters ──────────────────────────────────────
    @FXML public void applyFilters() {
        if (productListController == null) return;

        // Categories
        Set<String> cats = new HashSet<>();
        if (catHomeAssistants != null && catHomeAssistants.isSelected()) cats.add("Home Assistants");
        if (catSecurityBots   != null && catSecurityBots.isSelected())   cats.add("Security Bots");
        if (catEducational    != null && catEducational.isSelected())     cats.add("Educational");
        if (catCompanions     != null && catCompanions.isSelected())      cats.add("Companions");
        if (catIndustrial     != null && catIndustrial.isSelected())      cats.add("Industrial");

        // Price range
        double min = 0;
        double max = Double.MAX_VALUE;
        if (priceUnder200 != null && priceUnder200.isSelected()) { min = 0;   max = 200; }
        if (price200to400 != null && price200to400.isSelected()) { min = 200; max = 400; }
        if (price400to600 != null && price400to600.isSelected()) { min = 400; max = 600; }
        if (priceOver600  != null && priceOver600.isSelected())  { min = 600; max = Double.MAX_VALUE; }

        // Search
        String search = searchField != null ? searchField.getText() : "";

        productListController.applyFilters(cats, min, max, search);
    }

    @FXML public void clearFilters() {
        if (catAllRobots     != null) catAllRobots.setSelected(false);
        if (catHomeAssistants != null) catHomeAssistants.setSelected(false);
        if (catSecurityBots   != null) catSecurityBots.setSelected(false);
        if (catEducational    != null) catEducational.setSelected(false);
        if (catCompanions     != null) catCompanions.setSelected(false);
        if (catIndustrial     != null) catIndustrial.setSelected(false);
        if (priceUnder200     != null) priceUnder200.setSelected(false);
        if (price200to400     != null) price200to400.setSelected(false);
        if (price400to600     != null) price400to600.setSelected(false);
        if (priceOver600      != null) priceOver600.setSelected(false);
        if (searchField       != null) searchField.clear();
        if (productListController != null)
            productListController.applyFilters(new HashSet<>(), 0, Double.MAX_VALUE, "");
    }
}