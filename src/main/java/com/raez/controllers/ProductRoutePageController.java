package com.raez.controllers;

import com.raez.dao.RoboticsCatalogDAO;
import com.raez.model.CartManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductRoutePageController implements Initializable {

    @FXML private Label productTitleLabel;
    @FXML private Label productPriceLabel;
    @FXML private Label productDescriptionLabel;
    @FXML private Label quantityLabel;
    @FXML private Label specsLabel;
    @FXML private Label shippingLabel;
    @FXML private Accordion detailsAccordion;
    @FXML private TitledPane specsPane;
    @FXML private TitledPane shippingPane;
    @FXML private Button orderButton;

    private final RoboticsCatalogDAO dao = new RoboticsCatalogDAO();
    private final CartManager cartManager = CartManager.getInstance();

    private int quantity = 1;
    private com.raez.model.Product product;
    private String categoryName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        quantityLabel.setText(String.valueOf(quantity));
        detailsAccordion.setExpandedPane(specsPane);
    }

    public void setProductId(int productId) {
        RoboticsCatalogDAO.ProductWithCategory row = dao.getProductWithCategoryById(productId);
        if (row == null) {
            productTitleLabel.setText("Product Not Found");
            productPriceLabel.setText("-");
            productDescriptionLabel.setText("No product was found for this route.");
            orderButton.setDisable(true);
            return;
        }
        this.product = row.product();
        this.categoryName = row.categoryName();
        bind();
    }

    @FXML
    private void decreaseQty() {
        if (quantity > 1) {
            quantity--;
            quantityLabel.setText(String.valueOf(quantity));
        }
    }

    @FXML
    private void increaseQty() {
        quantity++;
        quantityLabel.setText(String.valueOf(quantity));
    }

    @FXML
    private void initializeOrder() {
        if (product == null) return;
        for (int i = 0; i < quantity; i++) {
            cartManager.addItem(product.productID, product.name, product.price);
        }
        orderButton.setText("Order Initialized");
    }

    private void bind() {
        productTitleLabel.setText(product.name);
        productPriceLabel.setText(String.format("$%,.2f", product.price));
        productDescriptionLabel.setText(product.description == null ? "" : product.description);
        specsLabel.setText(buildSpecsText());
        shippingLabel.setText(buildShippingText());
    }

    private String buildSpecsText() {
        String category = categoryName == null ? "N/A" : categoryName;
        String collection = product.collection == null ? "Standalone" : product.collection;
        return "SKU: " + product.sku + "\n"
            + "Category: " + category + "\n"
            + "Collection: " + collection + "\n"
            + "Status: " + product.status;
    }

    private String buildShippingText() {
        return "Engineered freight packaging included.\n"
            + "Dispatch scheduling is confirmed after order initialization.\n"
            + "Lead times vary by category and deployment complexity.";
    }
}
