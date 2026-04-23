package com.raez.controllers;

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

public class ProductSideBarController implements Initializable {

    @FXML private Label categoryArrow;
    @FXML private Label priceArrow;
    @FXML private Label featuresArrow;
    @FXML private Label brandArrow;

    @FXML private VBox categoryPanel;
    @FXML private VBox pricePanel;
    @FXML private VBox featuresPanel;
    @FXML private VBox brandPanel;

    @FXML private CheckBox catRobots;
    @FXML private CheckBox catMiniRobots;
    @FXML private CheckBox catAccessories;
    @FXML private CheckBox catServices;

    @FXML private CheckBox priceUnder200;
    @FXML private CheckBox price200to400;
    @FXML private CheckBox price400to600;
    @FXML private CheckBox priceOver600;

    @FXML private TextField searchField;

    private ProductListController productListController;

    public void setProductListController(ProductListController c) {
        this.productListController = c;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

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

    @FXML public void applyFilters() {
        if (productListController == null) return;

        Set<String> cats = new HashSet<>();
        if (catRobots      != null && catRobots.isSelected())      cats.add("Robots");
        if (catMiniRobots  != null && catMiniRobots.isSelected())  cats.add("Mini Robots");
        if (catAccessories != null && catAccessories.isSelected()) cats.add("Accessories");
        if (catServices    != null && catServices.isSelected())    cats.add("Services");

        double min = 0;
        double max = Double.MAX_VALUE;
        if (priceUnder200 != null && priceUnder200.isSelected()) { min = 0;   max = 200; }
        if (price200to400 != null && price200to400.isSelected()) { min = 200; max = 400; }
        if (price400to600 != null && price400to600.isSelected()) { min = 400; max = 600; }
        if (priceOver600  != null && priceOver600.isSelected())  { min = 600; max = Double.MAX_VALUE; }

        String search = searchField != null ? searchField.getText() : "";

        productListController.applyFilters(cats, min, max, search);
    }

    @FXML public void clearFilters() {
        if (catRobots      != null) catRobots.setSelected(false);
        if (catMiniRobots  != null) catMiniRobots.setSelected(false);
        if (catAccessories != null) catAccessories.setSelected(false);
        if (catServices    != null) catServices.setSelected(false);
        if (priceUnder200     != null) priceUnder200.setSelected(false);
        if (price200to400     != null) price200to400.setSelected(false);
        if (price400to600     != null) price400to600.setSelected(false);
        if (priceOver600      != null) priceOver600.setSelected(false);
        if (searchField       != null) searchField.clear();
        if (productListController != null)
            productListController.applyFilters(new HashSet<>(), 0, Double.MAX_VALUE, "");
    }
}
