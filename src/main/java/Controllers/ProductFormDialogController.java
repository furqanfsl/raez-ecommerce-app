package Controllers;

import com.reaz.model.Category;
import com.reaz.model.Product;
import com.reaz.model.ProductImage;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
    @FXML private CheckBox    catHomeAssistants;
    @FXML private CheckBox    catSecurityBots;
    @FXML private CheckBox    catEducational;
    @FXML private CheckBox    catCompanions;
    @FXML private CheckBox    catIndustrial;
    @FXML private TextField   imageUrlField;
    @FXML private VBox        imageListBox;
    @FXML private RadioButton statusActive;
    @FXML private RadioButton statusInactive;
    @FXML private Button      submitBtn;
    @FXML private VBox        errorBox;
    @FXML private Label       errorLabel;

    private Product           editingProduct = null;
    private List<String>      images         = new ArrayList<>();
    private Consumer<Product> onSubmit;
    private Runnable          onClose;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

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
            refreshImageList();

            // Categories
            List<String> catNames = new ArrayList<>();
            for (Category c : product.categories) catNames.add(c.categoryName);
            if (catHomeAssistants != null) catHomeAssistants.setSelected(catNames.contains("Home Assistants"));
            if (catSecurityBots   != null) catSecurityBots.setSelected(catNames.contains("Security Bots"));
            if (catEducational    != null) catEducational.setSelected(catNames.contains("Educational"));
            if (catCompanions     != null) catCompanions.setSelected(catNames.contains("Companions"));
            if (catIndustrial     != null) catIndustrial.setSelected(catNames.contains("Industrial"));

            // Status
            boolean inactive = "INACTIVE".equalsIgnoreCase(product.status);
            if (statusInactive != null) statusInactive.setSelected(inactive);
            if (statusActive   != null) statusActive.setSelected(!inactive);
        } else {
            // Add mode
            if (dialogTitle != null) dialogTitle.setText("Add New Product");
            if (submitBtn   != null) submitBtn.setText("Add Product");
            if (statusActive != null) statusActive.setSelected(true);
        }
    }

    @FXML private void handleAddImage() {
        if (imageUrlField == null) return;
        String url = imageUrlField.getText().trim();
        if (!url.isEmpty()) {
            images.add(url);
            imageUrlField.clear();
            refreshImageList();
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

            String url = images.get(i);
            Label urlLabel = new Label(url.length() > 55 ? url.substring(0, 55) + "..." : url);
            urlLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
            HBox.setHgrow(urlLabel, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: #dc2626; -fx-cursor: hand; -fx-font-size: 12;");
            removeBtn.setOnAction(e -> { images.remove(idx); refreshImageList(); });

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
        if (catHomeAssistants != null && catHomeAssistants.isSelected()) catNames.add("Home Assistants");
        if (catSecurityBots   != null && catSecurityBots.isSelected())   catNames.add("Security Bots");
        if (catEducational    != null && catEducational.isSelected())     catNames.add("Educational");
        if (catCompanions     != null && catCompanions.isSelected())      catNames.add("Companions");
        if (catIndustrial     != null && catIndustrial.isSelected())      catNames.add("Industrial");

        // Build Product object
        Product p = new Product();
        if (editingProduct != null) {
            p.productID = editingProduct.productID;
            p.sku = editingProduct.sku;
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
        boolean anyCat = (catHomeAssistants != null && catHomeAssistants.isSelected())
                      || (catSecurityBots   != null && catSecurityBots.isSelected())
                      || (catEducational    != null && catEducational.isSelected())
                      || (catCompanions     != null && catCompanions.isSelected())
                      || (catIndustrial     != null && catIndustrial.isSelected());
        if (!anyCat) errors.add("At least one category must be selected");
        return errors;
    }
}