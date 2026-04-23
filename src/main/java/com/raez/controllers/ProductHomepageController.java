package com.raez.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductHomepageController implements Initializable {

    @FXML private ScrollPane mainScrollPane;
    @FXML private Button scrollToTopBtn;

    @FXML private ProductSideBarController sidebarController;
    @FXML private ProductListController productListController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ShopScrollChrome.wire(mainScrollPane, scrollToTopBtn);
        if (sidebarController != null && productListController != null) {
            sidebarController.setProductListController(productListController);
        }
    }
}
