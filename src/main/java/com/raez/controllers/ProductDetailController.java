package com.raez.controllers;

import com.raez.db.DBConnection;
import com.raez.model.CartManager;
import com.raez.model.FavouritesManager;
import com.raez.model.NavigationRouter;
import com.raez.model.Product;
import com.raez.model.User;
import com.raez.util.ProductImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProductDetailController implements Initializable {

    @FXML private Label     productNameLabel;
    @FXML private Label     categoryLabel;
    @FXML private Label     priceLabel;
    @FXML private Label     stockLabel;
    @FXML private Label     descriptionLabel;
    @FXML private Label     reviewsTitleLabel;
    @FXML private Label     reviewCountLabel;
    @FXML private Label     avgRatingStarsLabel;
    @FXML private Label     avgRatingValueLabel;
    @FXML private Button    addToCartBtn;
    @FXML private Button    buyNowBtn;
    @FXML private Button    favouriteBtn;
    @FXML private ImageView productImageView;
    @FXML private Label     imagePlaceholder;
    @FXML private StackPane imageContainer;
    @FXML private VBox      reviewsContainer;
    @FXML private ScrollPane mainScrollPane;
    @FXML private Button    scrollToTopBtn;

    private Product product;

    private final CartManager       cartManager = CartManager.getInstance();
    private final FavouritesManager favManager  = FavouritesManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ShopScrollChrome.wire(mainScrollPane, scrollToTopBtn);
        if (imageContainer != null && productImageView != null) {
            productImageView.fitWidthProperty().bind(imageContainer.widthProperty());
        }
    }

    public void setProduct(Product product) {
        this.product = product;
        populateInfo();
        loadImage();
        loadReviews();
    }

    // ── UI population ──────────────────────────────────────────────────────

    private void populateInfo() {
        productNameLabel.setText(product.name);
        categoryLabel.setText(product.getCategoryNames().toUpperCase());
        priceLabel.setText(String.format("£%.2f", product.price));

        boolean inStock = product.stock > 0;
        if (inStock) {
            stockLabel.setText("✓  In Stock  (" + product.stock + " available)");
            stockLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
        } else {
            stockLabel.setText("✕  Out of Stock");
            stockLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
            addToCartBtn.setDisable(true);
            buyNowBtn.setDisable(true);
        }

        String desc = (product.description != null && !product.description.isBlank())
            ? product.description
            : "No description available for this product.";
        descriptionLabel.setText(desc);

        updateFavBtn(favManager.isFavourite(product.productID));
        populateRating();
    }

    private void populateRating() {
        if (avgRatingStarsLabel == null) return;
        if (product.reviewCount == 0) {
            avgRatingStarsLabel.setText("☆☆☆☆☆");
            avgRatingStarsLabel.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 16;");
            if (avgRatingValueLabel != null) avgRatingValueLabel.setText("No ratings yet");
        } else {
            int full = (int) Math.round(product.avgRating);
            StringBuilder stars = new StringBuilder();
            for (int i = 1; i <= 5; i++) stars.append(i <= full ? '★' : '☆');
            avgRatingStarsLabel.setText(stars.toString());
            avgRatingStarsLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16;");
            if (avgRatingValueLabel != null)
                avgRatingValueLabel.setText(String.format("%.1f", product.avgRating));
        }
    }

    private void loadImage() {
        String path = product.getPrimaryImage();
        if (path == null || path.isBlank()) return;
        Thread t = new Thread(() -> {
            Image img = ProductImageUtil.loadFromProductPath(getClass(), path);
            if (img != null && !img.isError()) {
                Platform.runLater(() -> {
                    productImageView.setImage(img);
                    productImageView.setVisible(true);
                    imagePlaceholder.setVisible(false);
                    imagePlaceholder.setManaged(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void loadReviews() {
        Thread t = new Thread(() -> {
            List<ReviewRow> rows = fetchReviews();
            Platform.runLater(() -> renderReviews(rows));
        });
        t.setDaemon(true);
        t.start();
    }

    private List<ReviewRow> fetchReviews() {
        String sql =
            "SELECT rr.rating, rr.comment, rr.createdAt, c.name AS reviewer " +
            "FROM reviews_reviews rr " +
            "JOIN customers c ON c.customerID = rr.customerID " +
            "WHERE rr.productID = ? AND rr.status = 'ACTIVE' " +
            "ORDER BY rr.createdAt DESC LIMIT 10";
        List<ReviewRow> out = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, product.productID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new ReviewRow(
                    rs.getInt("rating"),
                    rs.getString("comment"),
                    rs.getString("reviewer"),
                    rs.getString("createdAt")
                ));
            }
        } catch (SQLException e) {
            System.err.println("ProductDetailController.fetchReviews: " + e.getMessage());
        }
        return out;
    }

    private void renderReviews(List<ReviewRow> rows) {
        reviewsContainer.getChildren().clear();
        String countText = "(" + rows.size() + " review" + (rows.size() != 1 ? "s" : "") + ")";
        if (reviewCountLabel    != null) reviewCountLabel.setText(countText);
        if (reviewsTitleLabel   != null) reviewsTitleLabel.setText("Customer Reviews (" + rows.size() + ")");

        if (rows.isEmpty()) {
            Label empty = new Label("No reviews yet for this product.");
            empty.setStyle("-fx-font-size: 14; -fx-text-fill: #9ca3af;");
            reviewsContainer.getChildren().add(empty);
            return;
        }

        for (ReviewRow r : rows) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;" +
                          "-fx-border-radius: 8; -fx-background-radius: 8;");

            // Header: stars + name + date
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            String stars = "★".repeat(r.rating) + "☆".repeat(5 - r.rating);
            Label starsLabel = new Label(stars);
            starsLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15;");

            Label nameLabel = new Label(r.reviewer != null ? r.reviewer : "Customer");
            nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            String dateStr = (r.createdAt != null && r.createdAt.length() >= 10)
                ? r.createdAt.substring(0, 10) : "";
            Label dateLabel = new Label(dateStr);
            dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9ca3af;");

            header.getChildren().addAll(starsLabel, nameLabel, spacer, dateLabel);

            Label comment = new Label(r.comment);
            comment.setWrapText(true);
            comment.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");

            card.getChildren().addAll(header, comment);
            reviewsContainer.getChildren().add(card);
        }
    }

    // ── Button handlers ────────────────────────────────────────────────────

    @FXML
    private void handleAddToCart() {
        cartManager.addItem(product.productID, product.name, product.price);
        addToCartBtn.setText("Added ✓");
        addToCartBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;" +
                              "-fx-font-size: 14; -fx-font-weight: bold;" +
                              "-fx-padding: 13 0; -fx-background-radius: 8; -fx-cursor: hand;");
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    addToCartBtn.setText("Add to Cart");
                    addToCartBtn.setStyle("-fx-background-color: #111827; -fx-text-fill: white;" +
                                         "-fx-font-size: 14; -fx-font-weight: bold;" +
                                         "-fx-padding: 13 0; -fx-background-radius: 8; -fx-cursor: hand;");
                });
            }
        }, 1500);
    }

    @FXML
    private void handleBuyNow() {
        cartManager.addItem(product.productID, product.name, product.price);
        User user = NavigationRouter.getInstance().getCurrentUser();
        if (user == null || !"customer".equals(user.roleName)) {
            ProductLoginModalLauncher.show(u -> Platform.runLater(() -> {
                if (u != null) NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
            }));
        } else {
            NavigationRouter.getInstance().navigateTo("/fxml/Cart.fxml");
        }
    }

    @FXML
    private void handleToggleFavourite() {
        updateFavBtn(favManager.toggle(product));
    }

    @FXML
    private void handleBack() {
        NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void updateFavBtn(boolean isFav) {
        if (favouriteBtn == null) return;
        if (isFav) {
            favouriteBtn.setText("♥  Saved to Favourites");
            favouriteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444;" +
                                  "-fx-border-color: #fecaca; -fx-border-radius: 8;" +
                                  "-fx-background-radius: 8; -fx-font-size: 13;" +
                                  "-fx-padding: 10 16; -fx-cursor: hand;");
        } else {
            favouriteBtn.setText("♡  Save to Favourites");
            favouriteBtn.setStyle("-fx-background-color: white; -fx-text-fill: #374151;" +
                                  "-fx-border-color: #d1d5db; -fx-border-radius: 8;" +
                                  "-fx-background-radius: 8; -fx-font-size: 13;" +
                                  "-fx-padding: 10 16; -fx-cursor: hand;");
        }
    }

    private record ReviewRow(int rating, String comment, String reviewer, String createdAt) {}
}
