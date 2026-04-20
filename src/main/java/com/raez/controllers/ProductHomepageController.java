package com.raez.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductHomepageController implements Initializable {

    @FXML private ProductSideBarController sidebarController;
    @FXML private ProductListController productListController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null && productListController != null) {
            sidebarController.setProductListController(productListController);
            System.out.println("Sidebar wired to ProductList ✓");
        } else {
            System.err.println("WARNING: sidebar=" + sidebarController + " productList=" + productListController);
        }
    }
}
