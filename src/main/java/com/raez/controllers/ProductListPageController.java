package com.raez.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductListPageController implements Initializable {

    @FXML private ScrollPane mainScrollPane;
    @FXML private Button scrollToTopBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ShopScrollChrome.wire(mainScrollPane, scrollToTopBtn);
    }
}
