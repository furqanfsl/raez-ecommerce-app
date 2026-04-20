package com.raez.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ProductFiltersController implements Initializable {

    @FXML private TextField  searchField;
    @FXML private MenuButton categoryBtn;
    @FXML private TextField  minPriceField;
    @FXML private TextField  maxPriceField;
    @FXML private MenuButton statusBtn;
    @FXML private Button     clearBtn;

    private String search   = "";
    private String category = "";
    private String minPrice = "";
    private String maxPrice = "";
    private String status   = "";

    private Consumer<FilterValues> onFilterChange;

    public void setOnFilterChange(Consumer<FilterValues> callback) {
        this.onFilterChange = callback;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML private void handleSearchChange() {
        search = searchField.getText();
        notifyChange();
    }

    @FXML private void handleCategoryChange(javafx.event.ActionEvent e) {
        String selected = ((javafx.scene.control.MenuItem) e.getSource()).getText();
        category = selected.equals("All categories") ? "" : selected;
        categoryBtn.setText(selected);
        notifyChange();
    }

    @FXML private void handlePriceChange() {
        minPrice = minPriceField.getText();
        maxPrice = maxPriceField.getText();
        notifyChange();
    }

    @FXML private void handleStatusChange(javafx.event.ActionEvent e) {
        String selected = ((javafx.scene.control.MenuItem) e.getSource()).getText();
        status = selected.equals("All statuses") ? "" : selected.toLowerCase();
        statusBtn.setText(selected);
        notifyChange();
    }

    @FXML private void handleClearFilters() {
        search = ""; category = ""; minPrice = ""; maxPrice = ""; status = "";
        searchField.clear();
        minPriceField.clear();
        maxPriceField.clear();
        categoryBtn.setText("All categories");
        statusBtn.setText("All statuses");
        clearBtn.setVisible(false);
        clearBtn.setManaged(false);
        notifyChange();
    }

    private void notifyChange() {
        boolean hasFilters = !search.isEmpty() || !category.isEmpty()
            || !minPrice.isEmpty() || !maxPrice.isEmpty() || !status.isEmpty();
        clearBtn.setVisible(hasFilters);
        clearBtn.setManaged(hasFilters);
        if (onFilterChange != null) {
            onFilterChange.accept(new FilterValues(search, category, minPrice, maxPrice, status));
        }
    }

    public static class FilterValues {
        public final String search, category, minPrice, maxPrice, status;
        public FilterValues(String search, String category,
                            String minPrice, String maxPrice, String status) {
            this.search   = search;
            this.category = category;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.status   = status;
        }
    }
}