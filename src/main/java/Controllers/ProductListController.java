package Controllers;

import com.reaz.model.FavouritesManager;
import com.reaz.model.Product;
import com.reaz.service.ProductService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.HashSet;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductListController implements Initializable {

    @FXML private VBox  productContainer;
    @FXML private Label itemCountLabel;

    private final ProductService service    = new ProductService();
    private final FavouritesManager favManager = FavouritesManager.getInstance();

    private Set<String> categoryFilters = new HashSet<>();
    private double      minPrice        = 0;
    private double      maxPrice        = Double.MAX_VALUE;
    private String      searchText      = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProducts();
    }

    public void applyFilters(Set<String> categories, double min, double max, String search) {
        this.categoryFilters = categories;
        this.minPrice        = min;
        this.maxPrice        = max;
        this.searchText      = search;
        loadProducts();
    }

    public void loadProducts() {
        List<Product> all = service.getActive();

        List<Product> products = all.stream()
            .filter(p -> {
                if (!searchText.isEmpty()) {
                    String q = searchText.toLowerCase();
                    boolean nameMatch = p.name.toLowerCase().contains(q);
                    boolean descMatch = p.description != null &&
                                        p.description.toLowerCase().contains(q);
                    if (!nameMatch && !descMatch) return false;
                }
                if (!categoryFilters.isEmpty()) {
                    Set<String> productCats = p.categories.stream()
                        .map(c -> c.categoryName)
                        .collect(Collectors.toSet());
                    if (categoryFilters.stream().noneMatch(productCats::contains)) return false;
                }
                if (p.price < minPrice || p.price > maxPrice) return false;
                return true;
            })
            .collect(Collectors.toList());

        System.out.println("Products loaded: " + products.size());

        if (itemCountLabel != null)
            itemCountLabel.setText(products.size() + " items");

        if (productContainer == null) {
            System.err.println("productContainer is NULL");
            return;
        }

        productContainer.getChildren().clear();

        if (products.isEmpty()) {
            Label empty = new Label("No products found. Go to Admin → Load Sample Data.");
            empty.setStyle("-fx-font-size: 14; -fx-text-fill: #6b7280; -fx-padding: 40;");
            productContainer.getChildren().add(empty);
            return;
        }

        // Use GridPane for perfectly equal card sizes
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(24);
        grid.setMaxWidth(Double.MAX_VALUE);

        // 4 equal columns
        for (int c = 0; c < 4; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }

        int col = 0, row = 0;
        for (Product product : products) {
            VBox card = buildCard(product);
            card.setMaxWidth(Double.MAX_VALUE);
            grid.add(card, col, row);
            col++;
            if (col == 4) { col = 0; row++; }
        }
        productContainer.getChildren().add(grid);
    }

    private VBox buildCard(Product product) {
        // Collect image URLs for this product
        List<String> imageUrls = new ArrayList<>();
        for (com.reaz.model.ProductImage img : product.images) {
            if (img.imageURL != null && !img.imageURL.isEmpty())
                imageUrls.add(img.imageURL);
        }
        if (imageUrls.isEmpty()) {
            String primary = product.getPrimaryImage();
            if (primary != null) imageUrls.add(primary);
        }

        // Track current image index
        final int[] currentIndex = {0};

        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(220);
        imagePane.setMinHeight(220);
        imagePane.setMaxHeight(220);
        imagePane.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 8;");

        Label placeholder = new Label("🤖");
        placeholder.setStyle("-fx-font-size: 48;");
        imagePane.getChildren().add(placeholder);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(false);
        imageView.setVisible(false);
        imagePane.getChildren().add(imageView);

        // Load first image
        if (!imageUrls.isEmpty()) {
            Thread t = new Thread(() -> loadImageWithRetry(imageUrls.get(0), imageView, placeholder, product.name, 3));
            t.setDaemon(true);
            t.start();
        }

        // Dot indicators (one per image)
        HBox dotsBox = new HBox(4);
        dotsBox.setAlignment(Pos.CENTER);
        StackPane.setAlignment(dotsBox, Pos.BOTTOM_CENTER);
        dotsBox.setTranslateY(-8);

        List<Label> dots = new ArrayList<>();
        if (imageUrls.size() > 1) {
            for (int i = 0; i < imageUrls.size(); i++) {
                Label dot = new Label("●");
                dot.setStyle(i == 0 ? dotStyle(true) : dotStyle(false));
                dots.add(dot);
                dotsBox.getChildren().add(dot);
            }
            imagePane.getChildren().add(dotsBox);
        }

        // Prev / Next buttons — only shown if more than 1 image
        if (imageUrls.size() > 1) {
            Button prevBtn = new Button("‹");
            prevBtn.setStyle(navBtnStyle());
            StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
            prevBtn.setTranslateX(6);
            prevBtn.setVisible(false);

            Button nextBtn = new Button("›");
            nextBtn.setStyle(navBtnStyle());
            StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
            nextBtn.setTranslateX(-6);

            // Show/hide nav buttons on hover
            imagePane.setOnMouseEntered(e -> {
                prevBtn.setVisible(currentIndex[0] > 0);
                nextBtn.setVisible(currentIndex[0] < imageUrls.size() - 1);
            });
            imagePane.setOnMouseExited(e -> {
                prevBtn.setVisible(false);
                nextBtn.setVisible(false);
            });

            prevBtn.setOnAction(e -> {
                if (currentIndex[0] > 0) {
                    currentIndex[0]--;
                    switchImage(imageUrls.get(currentIndex[0]), imageView, placeholder, product.name, dots, currentIndex[0]);
                    prevBtn.setVisible(currentIndex[0] > 0);
                    nextBtn.setVisible(true);
                }
            });

            nextBtn.setOnAction(e -> {
                if (currentIndex[0] < imageUrls.size() - 1) {
                    currentIndex[0]++;
                    switchImage(imageUrls.get(currentIndex[0]), imageView, placeholder, product.name, dots, currentIndex[0]);
                    nextBtn.setVisible(currentIndex[0] < imageUrls.size() - 1);
                    prevBtn.setVisible(true);
                }
            });

            imagePane.getChildren().addAll(prevBtn, nextBtn);
        }

        // Heart button
        boolean alreadyFav = favManager.isFavourite(product.productID);
        Button heartBtn = new Button(alreadyFav ? "♥" : "♡");
        heartBtn.setStyle(heartStyle(alreadyFav));
        StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
        heartBtn.setTranslateX(-8);
        heartBtn.setTranslateY(8);
        heartBtn.setOnAction(e -> {
            boolean nowFav = favManager.toggle(product);
            heartBtn.setText(nowFav ? "♥" : "♡");
            heartBtn.setStyle(heartStyle(nowFav));
        });
        imagePane.getChildren().add(heartBtn);

        Label nameLabel = new Label(product.name);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label categoryLabel = new Label(product.getCategoryNames());
        categoryLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");

        Label ratingLabel = new Label("★★★★★  4.8");
        ratingLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #f59e0b;");

        Label priceLabel = new Label(String.format("£%.2f", product.price));
        priceLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label stockLabel = new Label(product.stock > 0 ? "In Stock" : "Check Availability");
        stockLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a;");

        VBox info = new VBox(4, nameLabel, categoryLabel, ratingLabel, priceLabel, stockLabel);
        info.setPadding(new Insets(10, 4, 4, 4));
        info.setStyle("-fx-background-color: white;");

        VBox card = new VBox(0, imagePane, info);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private void switchImage(String url, ImageView imageView, Label placeholder,
                             String productName, List<Label> dots, int activeIndex) {
        // Update dots immediately
        Platform.runLater(() -> {
            for (int i = 0; i < dots.size(); i++)
                dots.get(i).setStyle(i == activeIndex ? dotStyle(true) : dotStyle(false));
        });
        // Load new image in background
        Thread t = new Thread(() -> loadImageWithRetry(url, imageView, placeholder, productName, 2));
        t.setDaemon(true);
        t.start();
    }

    private String dotStyle(boolean active) {
        return "-fx-font-size: 8; -fx-text-fill: " + (active ? "white" : "rgba(255,255,255,0.5)") + ";";
    }

    private String navBtnStyle() {
        return "-fx-background-color: rgba(255,255,255,0.85);" +
               "-fx-text-fill: #111827;" +
               "-fx-background-radius: 50;" +
               "-fx-border-color: transparent;" +
               "-fx-font-size: 16; -fx-font-weight: bold; -fx-cursor: hand;" +
               "-fx-min-width: 30; -fx-min-height: 30; -fx-padding: 0 7 1 7;" +
               "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),6,0,0,2);";
    }

    private String heartStyle(boolean active) {
        return "-fx-background-color: white; -fx-background-radius: 50;" +
               "-fx-border-color: transparent; -fx-font-size: 14;" +
               "-fx-text-fill: " + (active ? "#ef4444" : "#9ca3af") + "; -fx-cursor: hand;" +
               "-fx-min-width: 32; -fx-min-height: 32;" +
               "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),4,0,0,1);";
    }

    /**
     * Tries to load an image up to maxRetries times.
     * Waits 1.5s between attempts to handle temporary network issues.
     */
    private void loadImageWithRetry(String url, ImageView imageView,
                                    Label placeholder, String productName, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Use java.net.URL to open a connection with explicit timeout
                java.net.URLConnection conn = new java.net.URL(url).openConnection();
                conn.setConnectTimeout(5000);  // 5s connect timeout
                conn.setReadTimeout(8000);     // 8s read timeout
                conn.connect();

                // Now load via JavaFX Image
                Image img = new Image(url, true);

                // Wait for it to finish loading (block this background thread)
                while (img.getProgress() < 1.0 && !img.isError()) {
                    Thread.sleep(100);
                }

                if (!img.isError()) {
                    Platform.runLater(() -> {
                        imageView.setImage(img);
                        imageView.setVisible(true);
                        placeholder.setVisible(false);
                    });
                    return; // success — stop retrying
                }

                System.err.println("Image attempt " + attempt + " failed (error): " + productName);

            } catch (Exception e) {
                System.err.println("Image attempt " + attempt + " failed (" + e.getMessage() + "): " + productName);
            }

            // Wait before retrying
            if (attempt < maxRetries) {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            }
        }
        System.err.println("All image attempts failed for: " + productName);
    }
}