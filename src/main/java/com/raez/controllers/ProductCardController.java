package com.raez.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class ProductCardController implements Initializable {

    @FXML private ImageView productImage;
    @FXML private Label     productName;
    @FXML private Label     ratingLabel;
    @FXML private Label     reviewsLabel;
    @FXML private Label     priceLabel;
    @FXML private Label     originalPriceLabel;
    @FXML private Label     discountBadge;
    @FXML private Button    favouriteBtn;

    private boolean isFavourite = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Nothing needed on init — data set via setProduct()
    }

    // Called by the parent controller to fill in product data
    public void setProduct(String name, double price, double originalPrice,
                           String imageUrl, double rating, int reviews) {

        productName.setText(name);
        ratingLabel.setText(String.valueOf(rating));
        reviewsLabel.setText("(" + reviews + ")");
        priceLabel.setText("£" + String.format("%.2f", price));

        if (originalPrice > 0) {
            int discount = (int) Math.round(
                (originalPrice - price) / originalPrice * 100
            );
            originalPriceLabel.setText("£" + String.format("%.2f", originalPrice));
            originalPriceLabel.setVisible(true);
            originalPriceLabel.setManaged(true);

            discountBadge.setText("-" + discount + "%");
            discountBadge.setVisible(true);
            discountBadge.setManaged(true);
        }

        // Load image on background thread so UI stays smooth
        Thread imageThread = new Thread(() -> {
            Image img = new Image(imageUrl, 240, 320, false, true, true);
            javafx.application.Platform.runLater(() -> productImage.setImage(img));
        });
        imageThread.setDaemon(true);
        imageThread.start();
    }

    @FXML
    private void handleToggleFavourite() {
        isFavourite = !isFavourite;
        favouriteBtn.setText(isFavourite ? "♥" : "♡");
        if (isFavourite) {
            favouriteBtn.setStyle(
                "-fx-background-color: white; -fx-background-radius: 50; " +
                "-fx-border-color: transparent; -fx-font-size: 14; " +
                "-fx-cursor: hand; -fx-text-fill: #ef4444;"
            );
        } else {
            favouriteBtn.setStyle(
                "-fx-background-color: white; -fx-background-radius: 50; " +
                "-fx-border-color: transparent; -fx-font-size: 14; " +
                "-fx-cursor: hand; -fx-text-fill: #374151;"
            );
        }
    }

    @FXML
    private void handleCardClick() {
        System.out.println("Clicked: " + productName.getText());
        // TODO: open product detail page
    }
}