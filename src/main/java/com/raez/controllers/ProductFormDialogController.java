package com.raez.controllers;

import com.raez.model.Category;
import com.raez.model.Product;
import com.raez.model.ProductImage;
import com.raez.storage.ImageStorage;
import com.raez.storage.ImageStorageFactory;
import com.raez.util.ProductImageUtil;
import com.raez.util.Validators;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ProductFormDialogController implements Initializable {

    @FXML private Label       dialogTitle;
    @FXML private TextField   nameField;
    @FXML private TextArea    descriptionField;
    @FXML private TextField   priceField;
    @FXML private TextField   stockField;
    @FXML private ComboBox<String> collectionCombo;
    @FXML private CheckBox    catMainRobot;
    @FXML private CheckBox    catMiniRobot;
    @FXML private CheckBox    catAccessory;
    @FXML private CheckBox    catService;
    @FXML private TextField   selectedImageField;
    @FXML private VBox        imageListBox;
    @FXML private ImageView   imagePreview;
    @FXML private Label       imagePreviewPlaceholder;
    @FXML private StackPane   imagePreviewFrame;
    @FXML private RadioButton statusActive;
    @FXML private RadioButton statusInactive;
    @FXML private Button      submitBtn;
    @FXML private VBox        errorBox;
    @FXML private Label       errorLabel;

    private Product           editingProduct = null;
    private List<String>      images         = new ArrayList<>();
    private String            stagedImageUrl;
    private String            stagedImagePublicId;
    private Consumer<Product> onSubmit;
    private Runnable          onClose;
    private static final ImageStorage IMAGE_STORAGE = ImageStorageFactory.create();

    private static final java.util.List<String> COLLECTIONS = java.util.List.of(
        "None (Standalone)",
        "The Apex Series",
        "The Ledger Series",
        "The Velocity Series",
        "The Sentinel Series"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (collectionCombo != null) {
            collectionCombo.getItems().setAll(COLLECTIONS);
            collectionCombo.setValue("None (Standalone)");
        }
    }

    public void setup(Product product, Consumer<Product> onSubmit, Runnable onClose) {
        this.onSubmit       = onSubmit;
        this.onClose        = onClose;
        this.editingProduct = product;

        if (product != null) {
            // Edit mode — populate fields
            if (dialogTitle != null) dialogTitle.setText("Edit Product");
            if (submitBtn   != null) submitBtn.setText("Update Product");
            if (nameField   != null) nameField.setText(product.name);
            if (descriptionField != null)
                descriptionField.setText(product.description != null ? product.description : "");
            if (priceField  != null) priceField.setText(String.valueOf(product.price));
            if (stockField  != null) stockField.setText(String.valueOf(product.stock));

            // Images
            images = new ArrayList<>();
            for (ProductImage img : product.images) images.add(img.imageURL);
            if (images.isEmpty() && product.getImagePath() != null && !product.getImagePath().isBlank()) {
                images.add(product.getImagePath());
            }
            refreshImageList();
            if (selectedImageField != null && !images.isEmpty()) {
                String first = images.get(0);
                int slash = first.lastIndexOf('/');
                selectedImageField.setText(slash >= 0 ? first.substring(slash + 1) : first);
            }
            if (!images.isEmpty()) {
                showPreview(images.get(0));
            }

            // Categories
            List<String> catNames = new ArrayList<>();
            for (Category c : product.categories) catNames.add(c.categoryName);
            if (catMainRobot != null) catMainRobot.setSelected(catNames.contains("Main Robot"));
            if (catMiniRobot != null) catMiniRobot.setSelected(catNames.contains("Mini Robot"));
            if (catAccessory != null) catAccessory.setSelected(catNames.contains("Accessory"));
            if (catService   != null) catService.setSelected(catNames.contains("Service"));

            // Collection
            if (collectionCombo != null) {
                String col = product.collection;
                if (col != null && !col.isBlank() && COLLECTIONS.contains(col)) {
                    collectionCombo.setValue(col);
                } else {
                    collectionCombo.setValue("None (Standalone)");
                }
            }

            // Status
            boolean inactive = "INACTIVE".equalsIgnoreCase(product.status);
            if (statusInactive != null) statusInactive.setSelected(inactive);
            if (statusActive   != null) statusActive.setSelected(!inactive);
        } else {
            // Add mode
            if (dialogTitle != null) dialogTitle.setText("Add New Product");
            if (submitBtn   != null) submitBtn.setText("Add Product");
            if (statusActive != null) statusActive.setSelected(true);
            if (selectedImageField != null) selectedImageField.clear();
        }
    }

    @FXML
    private void handleBrowseLocalImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Product Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selected = chooser.showOpenDialog(submitBtn.getScene().getWindow());
        if (selected == null) return;
        try {
            String url;
            try {
                url = IMAGE_STORAGE.upload(selected);
                stagedImageUrl = url;
                stagedImagePublicId = IMAGE_STORAGE.getPublicIdFromUrl(url);
            } catch (Exception cloudFail) {
                System.err.println("Cloud upload failed, copying locally: " + cloudFail.getMessage());
                url = ProductImageUtil.copyImageToResources(selected);
                stagedImageUrl = null;
                stagedImagePublicId = null;
            }
            // New selection becomes primary — prepend so it lands at index 0.
            images.add(0, url);
            if (selectedImageField != null) selectedImageField.setText(selected.getName());
            refreshImageList();
            showPreview(url);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Failed to save image locally: " + e.getMessage());
            if (errorBox != null) {
                errorBox.setVisible(true);
                errorBox.setManaged(true);
            }
        }
    }

    private void showPreview(String imagePath) {
        if (imagePreview == null) return;
        Image img = ProductImageUtil.loadFromProductPath(getClass(), imagePath);
        if (img != null && !img.isError()) {
            imagePreview.setImage(img);
            if (imagePreviewPlaceholder != null) {
                imagePreviewPlaceholder.setVisible(false);
                imagePreviewPlaceholder.setManaged(false);
            }
        } else {
            imagePreview.setImage(null);
            if (imagePreviewPlaceholder != null) {
                imagePreviewPlaceholder.setVisible(true);
                imagePreviewPlaceholder.setManaged(true);
            }
        }
    }

    private void refreshImageList() {
        if (imageListBox == null) return;
        imageListBox.getChildren().clear();
        for (int i = 0; i < images.size(); i++) {
            final int idx = i;
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(i == 0 ? "Primary" : String.valueOf(i + 1));
            badge.setStyle(
                "-fx-background-color: " + (i == 0 ? "#111827" : "#e5e7eb") + ";" +
                "-fx-text-fill: "        + (i == 0 ? "white"   : "#374151") + ";" +
                "-fx-font-size: 10; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");

            String imagePath = images.get(i);
            String filename = imagePath;
            int slash = imagePath.lastIndexOf('/');
            if (slash >= 0 && slash < imagePath.length() - 1) filename = imagePath.substring(slash + 1);
            Label urlLabel = new Label(filename.length() > 55 ? filename.substring(0, 55) + "..." : filename);
            urlLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
            HBox.setHgrow(urlLabel, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: #dc2626; -fx-cursor: hand; -fx-font-size: 12;");
            removeBtn.setOnAction(e -> {
                images.remove(idx);
                refreshImageList();
                showPreview(images.isEmpty() ? null : images.get(0));
            });

            row.getChildren().addAll(badge, urlLabel, removeBtn);
            imageListBox.getChildren().add(row);
        }
    }

    @FXML private void handleSubmit() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            if (errorLabel != null) errorLabel.setText(String.join("\n", errors));
            if (errorBox   != null) { errorBox.setVisible(true); errorBox.setManaged(true); }
            return;
        }
        if (errorBox != null) { errorBox.setVisible(false); errorBox.setManaged(false); }

        // Build category list
        List<String> catNames = new ArrayList<>();
        if (catMainRobot != null && catMainRobot.isSelected()) catNames.add("Main Robot");
        if (catMiniRobot != null && catMiniRobot.isSelected()) catNames.add("Mini Robot");
        if (catAccessory != null && catAccessory.isSelected()) catNames.add("Accessory");
        if (catService   != null && catService.isSelected())   catNames.add("Service");

        // Boundary validation — catches anything the per-field validate() missed
        try {
            Validators.nonEmpty(nameField.getText(), 200, "Product name");
            Validators.positive(Double.parseDouble(priceField.getText().trim()), "Price");
            if (stockField != null && !stockField.getText().trim().isEmpty()) {
                int stock = Integer.parseInt(stockField.getText().trim());
                if (stock < 0) {
                    throw new IllegalArgumentException("Stock must be non-negative.");
                }
            }
        } catch (NumberFormatException nfe) {
            if (errorLabel != null) errorLabel.setText("Price and stock must be numbers.");
            if (errorBox   != null) { errorBox.setVisible(true); errorBox.setManaged(true); }
            return;
        } catch (IllegalArgumentException iae) {
            if (errorLabel != null) errorLabel.setText(iae.getMessage());
            if (errorBox   != null) { errorBox.setVisible(true); errorBox.setManaged(true); }
            return;
        }

        // Build Product object
        Product p = new Product();
        if (editingProduct != null) {
            p.productID = editingProduct.productID;
            p.sku = editingProduct.sku;
        }
        // Collection — set from combo; "None (Standalone)" means null
        if (collectionCombo != null) {
            String sel = collectionCombo.getValue();
            p.collection = (sel == null || sel.startsWith("None")) ? null : sel;
        } else if (editingProduct != null) {
            p.collection = editingProduct.collection;
        }
        p.name        = nameField.getText().trim();
        p.description = descriptionField != null ? descriptionField.getText().trim() : "";
        p.price       = Double.parseDouble(priceField.getText().trim());
        p.unitCost    = 0.0;
        p.categoryID  = null;
        p.stock       = stockField != null && !stockField.getText().trim().isEmpty()
                        ? Integer.parseInt(stockField.getText().trim()) : 0;
        p.status      = (statusInactive != null && statusInactive.isSelected()) ? "INACTIVE" : "ACTIVE";

        // Attach categories as Category objects (service will look them up by name)
        for (String name : catNames) {
            Category c = new Category();
            c.categoryName = name;
            c.isActive = 1;
            p.categories.add(c);
        }

        // Attach images as ProductImage objects
        for (int i = 0; i < images.size(); i++) {
            ProductImage img = new ProductImage();
            img.imageURL = images.get(i);
            img.isPrimary = (i == 0) ? 1 : 0;
            p.images.add(img);
        }
        // Persist directly on products row too.
        p.setImagePath(images.isEmpty() ? null : images.get(0));
        if (stagedImageUrl != null) {
            p.imageUrl = stagedImageUrl;
            p.imagePublicId = stagedImagePublicId;
        } else if (editingProduct != null) {
            // Preserve existing cloud refs when no new image was uploaded
            p.imageUrl = editingProduct.imageUrl;
            p.imagePublicId = editingProduct.imagePublicId;
        }

        if (onSubmit != null) onSubmit.accept(p);
        if (onClose  != null) onClose.run();
    }

    @FXML private void handleClose() {
        if (onClose != null) onClose.run();
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (nameField == null || nameField.getText().trim().isEmpty())
            errors.add("Product name is required");
        if (priceField != null) {
            String pt = priceField.getText().trim();
            if (pt.isEmpty()) {
                errors.add("Price is required");
            } else {
                try {
                    if (Double.parseDouble(pt) <= 0) errors.add("Price must be positive");
                } catch (NumberFormatException e) {
                    errors.add("Price must be a valid number");
                }
            }
        }
        if (stockField != null && !stockField.getText().trim().isEmpty()) {
            try {
                if (Integer.parseInt(stockField.getText().trim()) < 0)
                    errors.add("Stock must be non-negative");
            } catch (NumberFormatException e) {
                errors.add("Stock must be a valid whole number");
            }
        }
        boolean anyCat = (catMainRobot != null && catMainRobot.isSelected())
                      || (catMiniRobot != null && catMiniRobot.isSelected())
                      || (catAccessory != null && catAccessory.isSelected())
                      || (catService   != null && catService.isSelected());
        if (!anyCat) errors.add("At least one category must be selected");
        return errors;
    }
}