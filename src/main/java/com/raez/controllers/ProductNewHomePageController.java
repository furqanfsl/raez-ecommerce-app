package com.raez.controllers;

import com.raez.model.NavigationRouter;
import com.raez.model.Product;
import com.raez.service.ProductService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProductNewHomePageController implements Initializable {

    @FXML private ImageView heroImage;
    @FXML private StackPane heroPane;
    @FXML private StackPane robotContainer;
    @FXML private Group     robotGroup;
    @FXML private VBox      heroTextBox;
    @FXML private Rectangle eyeLeft, eyeRight, pulseBar, armLeft, armRight;
    @FXML private Polygon   headShape;
    @FXML private Circle    reactorCore, antennaTip, backGlow;
    @FXML private VBox      stripsContainer;

    private final ProductService productService = new ProductService();

    // Strip filter state (applied across all four strips)
    private String filterSort  = "Newest";
    private String filterPrice = "All prices";
    private String filterQuery = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHeroImage();
        startRobotAnimation();
        buildCategoryStrips();
        Platform.runLater(this::playHeroEntrance);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero image (low-opacity backdrop)
    // ═══════════════════════════════════════════════════════════════════

    private void loadHeroImage() {
        if (heroImage == null) return;
        URL imageUrl = getClass().getResource("/images/hero.png");
        if (imageUrl == null) return;
        Thread t = new Thread(() -> {
            try {
                Image img = new Image(imageUrl.toExternalForm(), true);
                while (img.getProgress() < 1.0 && !img.isError()) Thread.sleep(80);
                if (!img.isError()) Platform.runLater(() -> heroImage.setImage(img));
            } catch (Exception e) {
                System.err.println("Hero image load failed: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero entrance animation (text slides in from left, robot from right)
    // ═══════════════════════════════════════════════════════════════════

    private void playHeroEntrance() {
        if (heroTextBox == null || robotContainer == null) return;

        heroTextBox.setOpacity(0);
        heroTextBox.setTranslateX(-30);
        robotContainer.setOpacity(0);
        robotContainer.setTranslateX(30);

        FadeTransition textFade = new FadeTransition(Duration.millis(700), heroTextBox);
        textFade.setFromValue(0); textFade.setToValue(1);
        TranslateTransition textSlide = new TranslateTransition(Duration.millis(700), heroTextBox);
        textSlide.setFromX(-30); textSlide.setToX(0);
        textSlide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition robotFade = new FadeTransition(Duration.millis(700), robotContainer);
        robotFade.setFromValue(0); robotFade.setToValue(1);
        TranslateTransition robotSlide = new TranslateTransition(Duration.millis(700), robotContainer);
        robotSlide.setFromX(30); robotSlide.setToX(0);
        robotSlide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(textFade, textSlide, robotFade, robotSlide).play();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Transformer robot animation
    // ═══════════════════════════════════════════════════════════════════

    private void startRobotAnimation() {
        if (eyeLeft == null || eyeRight == null) return;

        // Eye pulse
        Timeline eyePulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(eyeLeft.opacityProperty(),  1.0),
                new KeyValue(eyeRight.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(700),
                new KeyValue(eyeLeft.opacityProperty(),  0.25),
                new KeyValue(eyeRight.opacityProperty(), 0.25)),
            new KeyFrame(Duration.millis(1400),
                new KeyValue(eyeLeft.opacityProperty(),  1.0),
                new KeyValue(eyeRight.opacityProperty(), 1.0))
        );
        eyePulse.setCycleCount(Timeline.INDEFINITE);
        eyePulse.play();

        // Reactor core pulsing
        if (reactorCore != null) {
            Timeline reactorPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(reactorCore.scaleXProperty(), 1.0),
                    new KeyValue(reactorCore.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(900),
                    new KeyValue(reactorCore.scaleXProperty(), 1.15),
                    new KeyValue(reactorCore.scaleYProperty(), 1.15)),
                new KeyFrame(Duration.millis(1800),
                    new KeyValue(reactorCore.scaleXProperty(), 1.0),
                    new KeyValue(reactorCore.scaleYProperty(), 1.0))
            );
            reactorPulse.setCycleCount(Timeline.INDEFINITE);
            reactorPulse.play();
        }

        // Antenna tip blink
        if (antennaTip != null) {
            Timeline antennaBlink = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(antennaTip.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600),
                    new KeyValue(antennaTip.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(1200),
                    new KeyValue(antennaTip.opacityProperty(), 1.0))
            );
            antennaBlink.setCycleCount(Timeline.INDEFINITE);
            antennaBlink.play();
        }

        // Scanning pulse bar (width sweep)
        if (pulseBar != null) {
            Timeline barScan = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pulseBar.widthProperty(), 12.0)),
                new KeyFrame(Duration.millis(1800),
                    new KeyValue(pulseBar.widthProperty(), 132.0)),
                new KeyFrame(Duration.millis(2800),
                    new KeyValue(pulseBar.widthProperty(), 12.0))
            );
            barScan.setCycleCount(Timeline.INDEFINITE);
            barScan.play();
        }

        // Head tilt — side-to-side rotation
        if (headShape != null) {
            RotateTransition headTilt = new RotateTransition(Duration.millis(3600), headShape);
            headTilt.setFromAngle(-5);
            headTilt.setToAngle(5);
            headTilt.setAutoReverse(true);
            headTilt.setCycleCount(RotateTransition.INDEFINITE);
            headTilt.setInterpolator(Interpolator.EASE_BOTH);
            headTilt.play();
        }

        // Arms — subtle transformer-style sway
        if (armLeft != null) {
            RotateTransition swayL = new RotateTransition(Duration.millis(2800), armLeft);
            swayL.setFromAngle(-4); swayL.setToAngle(6);
            swayL.setAutoReverse(true);
            swayL.setCycleCount(RotateTransition.INDEFINITE);
            swayL.setInterpolator(Interpolator.EASE_BOTH);
            swayL.play();
        }
        if (armRight != null) {
            RotateTransition swayR = new RotateTransition(Duration.millis(2800), armRight);
            swayR.setFromAngle(6); swayR.setToAngle(-4);
            swayR.setAutoReverse(true);
            swayR.setCycleCount(RotateTransition.INDEFINITE);
            swayR.setInterpolator(Interpolator.EASE_BOTH);
            swayR.play();
        }

        // Back glow — slow rotation
        if (backGlow != null) {
            RotateTransition glowSpin = new RotateTransition(Duration.seconds(16), backGlow);
            glowSpin.setFromAngle(0); glowSpin.setToAngle(360);
            glowSpin.setCycleCount(RotateTransition.INDEFINITE);
            glowSpin.setInterpolator(Interpolator.LINEAR);
            glowSpin.play();
        }

        // Gentle floating of the whole robot
        if (robotGroup != null) {
            TranslateTransition floatAnim = new TranslateTransition(Duration.millis(2800), robotGroup);
            floatAnim.setFromY(0);
            floatAnim.setToY(-14);
            floatAnim.setAutoReverse(true);
            floatAnim.setCycleCount(TranslateTransition.INDEFINITE);
            floatAnim.setInterpolator(Interpolator.EASE_BOTH);
            floatAnim.play();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Horizontal category strips
    // ═══════════════════════════════════════════════════════════════════

    private void buildCategoryStrips() {
        if (stripsContainer == null) return;
        stripsContainer.getChildren().clear();

        stripsContainer.getChildren().add(buildFilterBar());
        renderStrips();
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding: 4 0 18 0;");

        Label title = new Label("Browse the collection");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1E2939;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Search products…");
        search.setPrefWidth(200);
        search.setStyle("-fx-font-size: 12; -fx-padding: 6 10; -fx-background-radius: 6;" +
                        "-fx-border-color: #e5e7eb; -fx-border-radius: 6;");
        search.textProperty().addListener((o, a, b) -> {
            filterQuery = b == null ? "" : b.trim();
            renderStrips();
        });

        ComboBox<String> price = new ComboBox<>();
        price.getItems().addAll("All prices", "Under £200", "£200–£400", "£400–£600", "Over £600");
        price.setValue(filterPrice);
        price.setStyle("-fx-font-size: 12;");
        price.setOnAction(e -> { filterPrice = price.getValue(); renderStrips(); });

        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("Newest", "Price: Low to High", "Price: High to Low", "Name: A–Z");
        sort.setValue(filterSort);
        sort.setStyle("-fx-font-size: 12;");
        sort.setOnAction(e -> { filterSort = sort.getValue(); renderStrips(); });

        Label priceLbl = new Label("Price:");
        priceLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        Label sortLbl = new Label("Sort:");
        sortLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");

        bar.getChildren().addAll(title, spacer, search, priceLbl, price, sortLbl, sort);
        return bar;
    }

    private void renderStrips() {
        if (stripsContainer == null) return;
        // Remove everything except the filter bar (child 0)
        if (stripsContainer.getChildren().size() > 1) {
            stripsContainer.getChildren().remove(1, stripsContainer.getChildren().size());
        }

        List<Product> all;
        try {
            all = productService.getActive();
        } catch (Exception e) {
            System.err.println("ProductNewHomePageController: product load failed: " + e.getMessage());
            return;
        }

        stripsContainer.getChildren().add(buildStrip(
            "Robots", "Full-scale AI companions built for every room",
            filterByCategoryOrAll(all, "Robots", "Home Assistants", "Companions")));

        stripsContainer.getChildren().add(buildStrip(
            "Mini Robots", "Compact, portable robotics for learning & play",
            filterByCategoryOrAll(all, "Mini Robots", "Educational")));

        stripsContainer.getChildren().add(buildStrip(
            "Accessories", "Add-ons, mounts, and power packs for your fleet",
            filterByCategoryOrAll(all, "Accessories")));

        stripsContainer.getChildren().add(buildStrip(
            "Services", "Installation, maintenance, and AI training plans",
            filterByCategoryOrAll(all, "Services")));
    }

    private List<Product> filterByCategoryOrAll(List<Product> all, String... categoryNames) {
        List<String> wanted = List.of(categoryNames);
        double min = 0, max = Double.MAX_VALUE;
        switch (filterPrice) {
            case "Under £200" -> { min = 0;   max = 200; }
            case "£200–£400" -> { min = 200; max = 400; }
            case "£400–£600" -> { min = 400; max = 600; }
            case "Over £600" -> { min = 600; max = Double.MAX_VALUE; }
            default -> { /* All prices */ }
        }
        final double fMin = min, fMax = max;
        final String q = filterQuery == null ? "" : filterQuery.toLowerCase();

        List<Product> matches = all.stream()
            .filter(p -> p.categories != null &&
                         p.categories.stream().anyMatch(c -> wanted.contains(c.categoryName)))
            .filter(p -> p.price >= fMin && p.price <= fMax)
            .filter(p -> q.isEmpty()
                || (p.name != null && p.name.toLowerCase().contains(q))
                || (p.description != null && p.description.toLowerCase().contains(q)))
            .collect(Collectors.toList());

        Comparator<Product> cmp = switch (filterSort) {
            case "Price: Low to High"  -> Comparator.comparingDouble((Product p) -> p.price);
            case "Price: High to Low"  -> Comparator.comparingDouble((Product p) -> p.price).reversed();
            case "Name: A–Z"           -> Comparator.comparing(p -> p.name == null ? "" : p.name.toLowerCase());
            default                    -> Comparator.comparingInt((Product p) -> p.productID).reversed();
        };
        matches.sort(cmp);

        if (matches.size() > 12) matches = new ArrayList<>(matches.subList(0, 12));
        return matches;
    }

    private VBox buildStrip(String title, String subtitle, List<Product> products) {
        VBox strip = new VBox(10);
        strip.setStyle("-fx-padding: 18 0 18 0;");

        // Header row
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1E2939;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewAll = new Button("View all →");
        viewAll.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent;" +
            "-fx-text-fill: #1E2939; -fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 6 0 6 0;"
        );
        viewAll.setOnAction(e -> {
            ProductListController.setPendingCategory(title);
            navigateToHome();
        });

        header.getChildren().addAll(titleBox, spacer, viewAll);
        strip.getChildren().add(header);

        // Cards row — horizontal
        HBox cardRow = new HBox(14);
        cardRow.setStyle("-fx-padding: 6 2 12 2;");

        if (products.isEmpty()) {
            cardRow.getChildren().add(buildComingSoonCard(title));
        } else {
            for (Product p : products) cardRow.getChildren().add(buildMiniCard(p));
        }

        // Wrap in a ScrollPane with scrollbars hidden via CSS
        ScrollPane scroll = new ScrollPane(cardRow);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                        "-fx-background: transparent;");
        scroll.setPrefHeight(240);
        scroll.setMinHeight(240);

        // Overlay arrows (circular, half-transparent)
        Button leftBtn  = buildNavArrow("‹");
        Button rightBtn = buildNavArrow("›");

        StackPane viewport = new StackPane(scroll, leftBtn, rightBtn);
        StackPane.setAlignment(leftBtn,  Pos.CENTER_LEFT);
        StackPane.setAlignment(rightBtn, Pos.CENTER_RIGHT);
        leftBtn.setTranslateX(8);
        rightBtn.setTranslateX(-8);

        // Auto-scroll: sweep hvalue 0 → 1 and back, continuously, with ease-in-out
        Timeline autoScroll = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(scroll.hvalueProperty(), 0.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(14),
                new KeyValue(scroll.hvalueProperty(), 1.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(28),
                new KeyValue(scroll.hvalueProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        autoScroll.setCycleCount(Timeline.INDEFINITE);
        // Start after layout so hvalue is meaningful
        Platform.runLater(autoScroll::play);

        // Pause on hover; resume on exit
        viewport.setOnMouseEntered(e -> autoScroll.pause());
        viewport.setOnMouseExited (e -> autoScroll.play());

        // Manual step
        leftBtn.setOnAction(e -> {
            autoScroll.pause();
            scroll.setHvalue(Math.max(0.0, scroll.getHvalue() - 0.2));
        });
        rightBtn.setOnAction(e -> {
            autoScroll.pause();
            scroll.setHvalue(Math.min(1.0, scroll.getHvalue() + 0.2));
        });

        scroll.prefViewportWidthProperty().bind(stripsContainer.widthProperty());
        scroll.setMaxWidth(Double.MAX_VALUE);
        viewport.setMaxWidth(Double.MAX_VALUE);
        strip.setMaxWidth(Double.MAX_VALUE);

        strip.getChildren().add(viewport);
        return strip;
    }

    private Button buildNavArrow(String glyph) {
        Button b = new Button(glyph);
        b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92);" +
            "-fx-text-fill: #1E2939; -fx-font-size: 22; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-border-color: transparent;" +
            "-fx-min-width: 40; -fx-min-height: 40;" +
            "-fx-max-width: 40; -fx-max-height: 40;" +
            "-fx-padding: 0 0 3 0; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 2);"
        );
        return b;
    }

    private VBox buildMiniCard(Product product) {
        VBox card = new VBox(6);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setMinWidth(220);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;" +
            "-fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0.15, 0, 2);"
        );

        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(160);
        imagePane.setMinHeight(160);
        imagePane.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 10 10 0 0;");

        Label placeholder = new Label("🤖");
        placeholder.setStyle("-fx-font-size: 36;");
        imagePane.getChildren().add(placeholder);

        ImageView iv = new ImageView();
        iv.setFitWidth(220);
        iv.setFitHeight(160);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setVisible(false);
        imagePane.getChildren().add(iv);

        String primary = product.getPrimaryImage();
        if (primary != null && !primary.isEmpty()) {
            Thread t = new Thread(() -> {
                try {
                    Image img = new Image(primary, true);
                    while (img.getProgress() < 1.0 && !img.isError()) Thread.sleep(80);
                    if (!img.isError()) Platform.runLater(() -> {
                        iv.setImage(img);
                        iv.setVisible(true);
                        placeholder.setVisible(false);
                    });
                } catch (Exception ignore) {}
            });
            t.setDaemon(true);
            t.start();
        }

        VBox info = new VBox(4);
        info.setStyle("-fx-padding: 10 12 12 12;");
        Label name = new Label(product.name);
        name.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1E2939;");
        name.setWrapText(true);
        name.setMaxWidth(200);
        Label price = new Label(String.format("£%.2f", product.price));
        price.setStyle("-fx-font-size: 13; -fx-text-fill: #2D3E52; -fx-font-weight: bold;");
        info.getChildren().addAll(name, price);

        card.getChildren().addAll(imagePane, info);
        card.setOnMouseClicked(e -> NavigationRouter.getInstance().navigateToProductDetail(product));
        return card;
    }

    private VBox buildComingSoonCard(String category) {
        VBox card = new VBox(6);
        card.setPrefWidth(280);
        card.setMinHeight(220);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #f3f4f6, #e5e7eb);" +
            "-fx-background-radius: 10; -fx-border-color: #d1d5db;" +
            "-fx-border-radius: 10; -fx-border-width: 1; -fx-border-style: dashed;"
        );
        Label emoji = new Label("🛠");
        emoji.setStyle("-fx-font-size: 36;");
        Label title = new Label(category + " — Coming Soon");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1E2939;");
        Label body = new Label("Restocking this range shortly.");
        body.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        card.getChildren().addAll(emoji, title, body);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero button handlers & navigation
    // ═══════════════════════════════════════════════════════════════════

    @FXML
    private void handleExploreCollection() {
        ProductListController.setPendingCollection("Apex Automata");
        navigateToHome();
    }

    @FXML
    private void handleShopNow() {
        navigateToHome();
    }

    private void navigateToHome() {
        try {
            javafx.scene.Parent view = FXMLLoader.load(
                getClass().getResource("/fxml/ProductListPage.fxml"));
            heroPane.getScene().setRoot(view);
        } catch (Exception e) {
            System.err.println("ProductNewHomePageController: nav failed: " + e.getMessage());
        }
    }
}
