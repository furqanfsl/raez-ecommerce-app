package com.raez.controllers;

import com.raez.dao.RoboticsCatalogDAO;
import com.raez.model.CartManager;
import com.raez.model.NavigationRouter;
import com.raez.util.ProductImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CollectionPageController implements Initializable {

    @FXML private Label     collectionTitleLabel;
    @FXML private Label     mainRobotNameLabel;
    @FXML private Label     mainRobotPriceLabel;
    @FXML private Label     mainRobotDescriptionLabel;
    @FXML private Label     collectionEmptyLabel;
    @FXML private Label     mainRobotImagePlaceholder;
    @FXML private ImageView mainRobotImageView;
    @FXML private FlowPane  itemsGrid;
    @FXML private Button    mainRobotCartBtn;

    private final RoboticsCatalogDAO dao         = new RoboticsCatalogDAO();
    private final CartManager        cartManager = CartManager.getInstance();

    private RoboticsCatalogDAO.ProductWithCategory mainRobotRow = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no-op: data is provided via setCollectionSlug(...)
    }

    public void setCollectionSlug(String slug) {
        RoboticsCatalogDAO.CollectionInfo collection = dao.getCollectionBySlug(slug);
        if (collection == null) {
            collectionTitleLabel.setText("Collection Not Found");
            collectionEmptyLabel.setVisible(true);
            collectionEmptyLabel.setManaged(true);
            return;
        }

        collectionTitleLabel.setText(collection.name());
        List<RoboticsCatalogDAO.ProductWithCategory> items = dao.getProductsByCollection(collection.collectionID());

        RoboticsCatalogDAO.ProductWithCategory main = items.stream()
            .filter(i -> "Main Robot".equalsIgnoreCase(i.categoryName()))
            .findFirst()
            .orElse(null);

        this.mainRobotRow = main;
        if (main != null) {
            mainRobotNameLabel.setText(main.product().name);
            mainRobotPriceLabel.setText(String.format("£%,.2f", main.product().price));
            mainRobotDescriptionLabel.setText(main.product().description);
            // Load main robot image asynchronously
            String imgPath = main.product().getPrimaryImage();
            if (imgPath != null && !imgPath.isBlank() && mainRobotImageView != null) {
                Thread t = new Thread(() -> {
                    Image img = ProductImageUtil.loadFromProductPath(CollectionPageController.class, imgPath);
                    if (img != null && !img.isError()) {
                        Platform.runLater(() -> {
                            mainRobotImageView.setImage(img);
                            if (mainRobotImagePlaceholder != null) {
                                mainRobotImagePlaceholder.setVisible(false);
                            }
                        });
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        } else {
            mainRobotNameLabel.setText("No Main Robot");
            mainRobotPriceLabel.setText("");
            mainRobotDescriptionLabel.setText("No main robot is currently configured for this collection.");
        }

        itemsGrid.getChildren().clear();
        items.stream()
            .filter(i -> !"Main Robot".equalsIgnoreCase(i.categoryName()))
            .forEach(i -> itemsGrid.getChildren().add(buildCard(i)));

        boolean empty = itemsGrid.getChildren().isEmpty();
        collectionEmptyLabel.setVisible(empty);
        collectionEmptyLabel.setManaged(empty);
    }

    private VBox buildCard(RoboticsCatalogDAO.ProductWithCategory row) {
        // Image pane
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(160);
        imgPane.setMinHeight(160);
        imgPane.setStyle("-fx-background-color: #0a1525; -fx-background-radius: 8 8 0 0;");
        Label imgPlaceholder = new Label("No Image");
        imgPlaceholder.setStyle("-fx-text-fill: #38506a; -fx-font-size: 11;");
        ImageView imgView = new ImageView();
        imgView.setFitWidth(280);
        imgView.setFitHeight(160);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);
        imgPane.getChildren().addAll(imgPlaceholder, imgView);

        String rawPath = row.product().getPrimaryImage();
        if (rawPath != null && !rawPath.isBlank()) {
            Thread t = new Thread(() -> {
                Image img = ProductImageUtil.loadThumbnail(rawPath, 300, 300);
                if (img != null && !img.isError()) {
                    Platform.runLater(() -> {
                        imgView.setImage(img);
                        imgPlaceholder.setVisible(false);
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        }

        // Text content
        Label tag = new Label(row.categoryName() == null ? "Uncategorized" : row.categoryName().toUpperCase());
        tag.setStyle("-fx-text-fill: #00f5a0; -fx-font-size: 10; -fx-font-weight: bold;");

        Label name = new Label(row.product().name);
        name.setWrapText(true);
        name.setStyle("-fx-text-fill: #e6f1ff; -fx-font-size: 16; -fx-font-weight: bold;");

        Label price = new Label(String.format("£%,.2f", row.product().price));
        price.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 15; -fx-font-weight: bold;");

        String desc = row.product().description == null ? "" : row.product().description;
        if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
        Label snippet = new Label(desc);
        snippet.setWrapText(true);
        snippet.setStyle("-fx-text-fill: #9fb3c8; -fx-font-size: 12;");

        Button addToCart = new Button("Add to Cart");
        addToCart.setStyle(
            "-fx-background-color: #00e5ff; -fx-text-fill: #050a16;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        Button buyBtn = new Button("Buy");
        buyBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white;" +
            "-fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-padding: 8 14; -fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 8; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;"
        );
        HBox actions = new HBox(8, addToCart, buyBtn);
        actions.setPadding(new Insets(6, 0, 0, 0));

        int productID = row.product().productID;
        String productName = row.product().name;
        double productPrice = row.product().price;

        addToCart.setOnAction(e -> {
            cartManager.addItem(productID, productName, productPrice);
            addToCart.setText("Added ✓");
            addToCart.setStyle(
                "-fx-background-color: #00c87a; -fx-text-fill: white;" +
                "-fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;"
            );
        });
        buyBtn.setOnAction(e -> {
            cartManager.addItem(productID, productName, productPrice);
            NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
        });

        VBox info = new VBox(8, tag, name, price, snippet, actions);
        info.setPadding(new Insets(12, 14, 14, 14));

        VBox card = new VBox(0, imgPane, info);
        card.setPrefWidth(280);
        card.setMinHeight(200);
        card.setStyle(
            "-fx-background-color: #101a2a;" +
            "-fx-border-color: #1f3b5b;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof javafx.scene.control.Button ||
                  e.getTarget() instanceof javafx.scene.text.Text)) {
                NavigationRouter.getInstance().navigateToProductRoute(productID);
            }
        });
        return card;
    }

    @FXML
    private void handleMainRobotAddToCart() {
        if (mainRobotRow == null) return;
        cartManager.addItem(
            mainRobotRow.product().productID,
            mainRobotRow.product().name,
            mainRobotRow.product().price
        );
        if (mainRobotCartBtn != null) {
            mainRobotCartBtn.setText("Added ✓");
            mainRobotCartBtn.setStyle(
                "-fx-background-color: #00c87a; -fx-text-fill: white;" +
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"
            );
        }
    }

    @FXML
    private void handleMainRobotViewDetails() {
        // "Buy" CTA — drop into cart and route to checkout immediately
        // (kept method name for FXML compatibility).
        if (mainRobotRow == null) return;
        cartManager.addItem(
            mainRobotRow.product().productID,
            mainRobotRow.product().name,
            mainRobotRow.product().price
        );
        NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
    }
}
