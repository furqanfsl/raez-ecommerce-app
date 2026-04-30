package com.raez.controllers;

import com.raez.model.CartManager;
import com.raez.model.FavouritesManager;
import com.raez.model.NavigationRouter;
import com.raez.model.Product;
import com.raez.service.ProductService;
import com.raez.util.GlassPlaceholder;
import com.raez.util.ProductImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.HashSet;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductListController implements Initializable {

    @FXML private VBox  productContainer;
    @FXML private Label itemCountLabel;
    @FXML private Label titleLabel;
    @FXML private Label breadcrumbLabel;
    @FXML private MenuButton sortButton;

    private enum SortMode { RECOMMENDED, PRICE_ASC, PRICE_DESC, NAME_ASC, NEWEST }
    private SortMode sortMode = SortMode.RECOMMENDED;

    private final ProductService    service     = new ProductService();
    private final FavouritesManager favManager  = FavouritesManager.getInstance();
    private final CartManager       cartManager = CartManager.getInstance();

    // Pending state — set before navigating to this page for one-shot filter/search
    private static String pendingCategoryFilter   = null;
    private static String pendingSearchText       = null;
    private static String pendingCollectionFilter = null;

    public static void setPendingCategory(String category)     { pendingCategoryFilter   = category;   }
    public static void setPendingSearch(String search)         { pendingSearchText       = search;     }
    public static void setPendingCollection(String collection) { pendingCollectionFilter = collection; }

    private Set<String> categoryFilters = new HashSet<>();
    private String      collectionFilter = null;
    private double      minPrice        = 0;
    private double      maxPrice        = Double.MAX_VALUE;
    private String      searchText      = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (pendingCategoryFilter != null) {
            categoryFilters = new HashSet<>(java.util.Set.of(pendingCategoryFilter));
            pendingCategoryFilter = null;
        }
        if (pendingCollectionFilter != null) {
            collectionFilter = pendingCollectionFilter;
            pendingCollectionFilter = null;
        }
        if (pendingSearchText != null) {
            searchText      = pendingSearchText;
            pendingSearchText = null;
        }
        updateHeader();
        loadProducts();
    }

    private void updateHeader() {
        String title;
        String crumb;
        if (collectionFilter != null && !collectionFilter.isBlank()) {
            title = collectionFilter;
            crumb = collectionFilter;
        } else if (!categoryFilters.isEmpty()) {
            title = String.join(", ", categoryFilters);
            crumb = title;
        } else if (searchText != null && !searchText.isBlank()) {
            title = "Search: \"" + searchText + "\"";
            crumb = "Search Results";
        } else {
            title = "All Products";
            crumb = "All Products";
        }
        if (titleLabel != null)      titleLabel.setText(title);
        if (breadcrumbLabel != null) breadcrumbLabel.setText(crumb);
    }

    @FXML private void handleSortRecommended() { setSort(SortMode.RECOMMENDED, "Recommended"); }
    @FXML private void handleSortPriceAsc()    { setSort(SortMode.PRICE_ASC,   "Price: Low to High"); }
    @FXML private void handleSortPriceDesc()   { setSort(SortMode.PRICE_DESC,  "Price: High to Low"); }
    @FXML private void handleSortName()        { setSort(SortMode.NAME_ASC,    "Name: A–Z"); }
    @FXML private void handleSortNewest()      { setSort(SortMode.NEWEST,      "Newest"); }

    private void setSort(SortMode mode, String label) {
        this.sortMode = mode;
        if (sortButton != null) sortButton.setText(label);
        loadProducts();
    }

    public void applyFilters(Set<String> categories, double min, double max, String search) {
        this.categoryFilters = categories;
        this.minPrice        = min;
        this.maxPrice        = max;
        this.searchText      = search;
        updateHeader();
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
                if (collectionFilter != null && !collectionFilter.isBlank()) {
                    if (p.collection == null ||
                        !p.collection.equalsIgnoreCase(collectionFilter)) return false;
                }
                if (p.price < minPrice || p.price > maxPrice) return false;
                return true;
            })
            .collect(Collectors.toList());

        switch (sortMode) {
            case PRICE_ASC  -> products.sort(Comparator.comparingDouble(pp -> pp.price));
            case PRICE_DESC -> products.sort(Comparator.comparingDouble((Product pp) -> pp.price).reversed());
            case NAME_ASC   -> products.sort(Comparator.comparing(pp -> pp.name == null ? "" : pp.name.toLowerCase()));
            case NEWEST     -> products.sort(Comparator.comparing(
                                    (Product pp) -> pp.updatedAt == null ? "" : pp.updatedAt).reversed());
            case RECOMMENDED -> { /* keep service order */ }
        }

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

        // 3 equal columns
        for (int c = 0; c < 3; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.333);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }

        int col = 0, row = 0;
        for (Product product : products) {
            VBox card = buildCard(product);
            card.setMaxWidth(Double.MAX_VALUE);
            grid.add(card, col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
        productContainer.getChildren().add(grid);
    }

    private VBox buildCard(Product product) {
        // Collect image URLs — prefer cloud imageUrl, then product_images table.
        List<String> imageUrls = new ArrayList<>();
        if (product.imageUrl != null && !product.imageUrl.isBlank()) {
            imageUrls.add(product.imageUrl);
        }
        for (com.raez.model.ProductImage img : product.images) {
            if (img.imageURL != null && !img.imageURL.isEmpty()
                && !imageUrls.contains(img.imageURL))
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
        imagePane.setStyle("-fx-background-color: transparent; -fx-background-radius: 16 16 0 0;");

        // Glass placeholder: dark gradient + glowing product initials. Visible until
        // the real image loads (or permanently for products with no image at all).
        StackPane placeholderBox = GlassPlaceholder.buildTopRounded(product.name, 16, 40);
        placeholderBox.prefWidthProperty().bind(imagePane.widthProperty());
        placeholderBox.prefHeightProperty().bind(imagePane.heightProperty());

        imagePane.getChildren().add(placeholderBox);

        ImageView imageView = new ImageView();
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setVisible(false);
        // Clip to match the glass tile corner radius
        javafx.scene.shape.Rectangle imgClip = new javafx.scene.shape.Rectangle();
        imgClip.setArcWidth(32); imgClip.setArcHeight(32);
        imgClip.widthProperty().bind(imagePane.widthProperty());
        imgClip.heightProperty().bind(imagePane.heightProperty());
        imageView.setClip(imgClip);
        imagePane.getChildren().add(imageView);

        // Load first image
        if (!imageUrls.isEmpty()) {
            Thread t = new Thread(() -> loadImageWithRetry(imageUrls.get(0), imageView, placeholderBox, product.name, 3));
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
                    switchImage(imageUrls.get(currentIndex[0]), imageView, placeholderBox, product.name, dots, currentIndex[0]);
                    prevBtn.setVisible(currentIndex[0] > 0);
                    nextBtn.setVisible(true);
                }
            });

            nextBtn.setOnAction(e -> {
                if (currentIndex[0] < imageUrls.size() - 1) {
                    currentIndex[0]++;
                    switchImage(imageUrls.get(currentIndex[0]), imageView, placeholderBox, product.name, dots, currentIndex[0]);
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

        Label categoryLabel = new Label(product.getCategoryNames().toUpperCase());
        categoryLabel.setStyle(
            "-fx-font-size: 10; -fx-text-fill: #5eead4;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-weight: bold; -fx-letter-spacing: 2;");

        Label nameLabel = new Label(product.name);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setStyle(
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;");

        Label ratingLabel = new Label(formatRating(product.avgRating, product.reviewCount));
        ratingLabel.setStyle(
            "-fx-font-size: 12; -fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-text-fill: " + (product.reviewCount > 0 ? "#fbbf24" : "rgba(255,255,255,0.35)") + ";");

        Label priceLabel = new Label(String.format("£%,.2f", product.price));
        priceLabel.setStyle(
            "-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #38bdf8;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;");

        Label stockLabel = new Label(product.stock > 0 ? "● In stock" : "○ Out of stock");
        stockLabel.setStyle(
            "-fx-font-size: 11; -fx-font-family: 'Inter','Segoe UI',sans-serif; -fx-text-fill: " +
            (product.stock > 0 ? "#5eead4" : "#f87171") + ";");

        final int    pid = product.productID;
        final String pn  = product.name;
        final double pp  = product.price;

        final String cartBaseStyle =
            "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 11; -fx-font-weight: bold;" +
            "-fx-padding: 9 0; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 20; -fx-border-width: 0.5;";
        final String cartAcquiredStyle =
            "-fx-background-color: rgba(94,234,212,0.18); -fx-text-fill: #5eead4;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 11; -fx-font-weight: bold;" +
            "-fx-padding: 9 0; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(94,234,212,0.45); -fx-border-radius: 20; -fx-border-width: 0.5;";

        Button cartBtn = new Button(product.stock > 0 ? "Add to Cart" : "Out of Stock");
        cartBtn.setDisable(product.stock <= 0);
        cartBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cartBtn, Priority.ALWAYS);
        cartBtn.setStyle(cartBaseStyle);
        cartBtn.setOnAction(e -> {
            e.consume();
            cartManager.addItem(pid, pn, pp);
            cartBtn.setText("Added ✓");
            cartBtn.setStyle(cartAcquiredStyle);
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() {
                    Platform.runLater(() -> {
                        cartBtn.setText("Add to Cart");
                        cartBtn.setStyle(cartBaseStyle);
                    });
                }
            }, 1500);
        });

        // Buy Now — the bright liquid button
        Button buyNowBtn = new Button("Buy Now");
        buyNowBtn.setDisable(product.stock <= 0);
        buyNowBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buyNowBtn, Priority.ALWAYS);
        buyNowBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #0a0f1f;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 11; -fx-font-weight: bold;" +
            "-fx-padding: 9 0; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.6); -fx-border-radius: 20; -fx-border-width: 0.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(94,234,212,0.35), 12, 0.25, 0, 4);");
        buyNowBtn.setOnAction(e -> {
            e.consume();
            cartManager.addItem(pid, pn, pp);
            com.raez.model.User user = NavigationRouter.getInstance().getCurrentUser();
            if (user == null || !"customer".equals(user.roleName)) {
                ProductLoginModalLauncher.show(u -> Platform.runLater(() -> {
                    if (u != null) NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
                }));
            } else {
                NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
            }
        });

        HBox btnRow = new HBox(6, cartBtn, buyNowBtn);
        btnRow.setMaxWidth(Double.MAX_VALUE);

        VBox info = new VBox(6, categoryLabel, nameLabel, ratingLabel, priceLabel, stockLabel, btnRow);
        info.setPadding(new Insets(14, 14, 14, 14));
        info.setStyle("-fx-background-color: transparent;");

        final String cardBaseStyle =
            "-fx-background-color: rgba(13,17,30,0.72);" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-radius: 16; -fx-background-radius: 16; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 18, 0.25, 0, 8);";
        final String cardHoverStyle =
            "-fx-background-color: rgba(30,38,58,0.85);" +
            "-fx-border-color: rgba(94,234,212,0.55);" +
            "-fx-border-radius: 16; -fx-background-radius: 16; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(56,189,248,0.38), 24, 0.45, 0, 0);";

        VBox card = new VBox(0, imagePane, info);
        card.setStyle(cardBaseStyle);
        card.setOnMouseEntered(e2 -> card.setStyle(cardHoverStyle));
        card.setOnMouseExited(e2 -> card.setStyle(cardBaseStyle));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);

        // Card click → product detail (ignore clicks on buttons)
        card.setOnMouseClicked(e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            while (target != null && target != card) {
                if (target instanceof Button) return;
                target = target.getParent();
            }
            NavigationRouter.getInstance().navigateByPath("/products/" + product.productID);
        });

        return card;
    }

    private void switchImage(String url, ImageView imageView, javafx.scene.Node placeholder,
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
        return "-fx-background-color: rgba(10,15,31,0.75); -fx-background-radius: 50;" +
               "-fx-border-color: " + (active ? "#f87171" : "rgba(255,255,255,0.25)") +
               "; -fx-border-radius: 50; -fx-border-width: 0.5;" +
               "-fx-font-size: 14;" +
               "-fx-text-fill: " + (active ? "#f87171" : "rgba(255,255,255,0.75)") + "; -fx-cursor: hand;" +
               "-fx-min-width: 32; -fx-min-height: 32;" +
               "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.45),6,0.2,0,2);";
    }

    private String formatRating(double avg, int count) {
        if (count == 0) return "No reviews yet";
        int full  = (int) Math.round(avg);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= full ? '★' : '☆');
        sb.append(String.format("  %.1f  (%d)", avg, count));
        return sb.toString();
    }

    /**
     * Tries to load an image up to maxRetries times.
     * Waits 1.5s between attempts to handle temporary network issues.
     */
    private void loadImageWithRetry(String url, ImageView imageView,
                                    javafx.scene.Node placeholder, String productName, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Image img = ProductImageUtil.loadThumbnail(url, 300, 300);

                if (img != null && !img.isError()) {
                    Platform.runLater(() -> {
                        imageView.setImage(img);
                        imageView.setVisible(true);
                        placeholder.setVisible(false);
                    });
                    return; // success — stop retrying
                }

                System.err.println("Image attempt " + attempt + " failed (not found/error): " + productName);

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