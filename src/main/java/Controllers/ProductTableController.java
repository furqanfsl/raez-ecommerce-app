package Controllers;

import com.reaz.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ProductTableController {

    @FXML private TableView<Product>           productTable;
    @FXML private TableColumn<Product, String> imageCol;
    @FXML private TableColumn<Product, String> nameCol;
    @FXML private TableColumn<Product, String> categoryCol;
    @FXML private TableColumn<Product, String> priceCol;
    @FXML private TableColumn<Product, String> stockCol;
    @FXML private TableColumn<Product, String> statusCol;
    @FXML private TableColumn<Product, Void>   actionsCol;

    private Consumer<Product>           onEdit;
    private IntConsumer                 onDelete;
    private BiConsumer<Integer, String> onToggleStatus;

    @FXML
    public void initialize() {

        // Image col
        if (imageCol != null)
            imageCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPrimaryImage() != null ? "🖼" : "🤖"));

        // Name col
        if (nameCol != null)
            nameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().name));

        // Categories col — first category as pill, +N for extras, fixed height
        if (categoryCol != null) {
            categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategoryNames()));
            categoryCol.setCellFactory(col -> new TableCell<>() {
                private final Label badge = new Label();
                {
                    badge.setStyle(
                        "-fx-background-color: #f3f4f6;" +
                        "-fx-text-fill: #374151;" +
                        "-fx-font-size: 11;" +
                        "-fx-padding: 3 8 3 8;" +
                        "-fx-background-radius: 20;");
                }
                @Override
                protected void updateItem(String cats, boolean empty) {
                    super.updateItem(cats, empty);
                    if (empty || cats == null || cats.isBlank()) {
                        setGraphic(null); setText(null); return;
                    }
                    String[] parts = cats.split(",");
                    badge.setText(parts[0].trim() + (parts.length > 1 ? " +" + (parts.length - 1) : ""));
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // Price col
        if (priceCol != null)
            priceCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("£%.2f", d.getValue().price)));

        // Stock col
        if (stockCol != null)
            stockCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().stock)));

        // Status col — black pill for active, grey for inactive
        if (statusCol != null) {
            statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().status));
            statusCol.setCellFactory(col -> new TableCell<>() {
                private final Label badge = new Label();
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) { setGraphic(null); return; }
                    boolean active = "ACTIVE".equalsIgnoreCase(status);
                    badge.setText(status.toLowerCase());
                    badge.setStyle(
                        "-fx-background-color: " + (active ? "#111827" : "#e5e7eb") + ";" +
                        "-fx-text-fill: "        + (active ? "white"   : "#6b7280") + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-padding: 3 10 3 10;" +
                        "-fx-background-radius: 20;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // Actions col — icon buttons ✏ 🔥 🗑
        if (actionsCol != null) {
            actionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn   = new Button("Edit");
                private final Button toggleBtn = new Button("Toggle");
                private final Button deleteBtn = new Button("Delete");
                private final HBox   box       = new HBox(6, editBtn, toggleBtn, deleteBtn);

                {
                    box.setAlignment(Pos.CENTER_LEFT);

                    editBtn.setStyle(
                        "-fx-background-color: #f3f4f6; -fx-text-fill: #111827;" +
                        "-fx-border-color: #e5e7eb; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                    toggleBtn.setStyle(
                        "-fx-background-color: #fff7ed; -fx-text-fill: #ea580c;" +
                        "-fx-border-color: #fed7aa; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                    deleteBtn.setStyle(
                        "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626;" +
                        "-fx-border-color: #fecaca; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;");

                    editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                        "-fx-background-color: #e5e7eb; -fx-text-fill: #111827;" +
                        "-fx-border-color: #d1d5db; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
                    editBtn.setOnMouseExited(e -> editBtn.setStyle(
                        "-fx-background-color: #f3f4f6; -fx-text-fill: #111827;" +
                        "-fx-border-color: #e5e7eb; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));

                    toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle(
                        "-fx-background-color: #fed7aa; -fx-text-fill: #c2410c;" +
                        "-fx-border-color: #fb923c; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
                    toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle(
                        "-fx-background-color: #fff7ed; -fx-text-fill: #ea580c;" +
                        "-fx-border-color: #fed7aa; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));

                    deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                        "-fx-background-color: #fecaca; -fx-text-fill: #b91c1c;" +
                        "-fx-border-color: #f87171; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
                    deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                        "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626;" +
                        "-fx-border-color: #fecaca; -fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 11; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));

                    editBtn.setOnAction(e -> {
                        Product p = getTableView().getItems().get(getIndex());
                        if (onEdit != null) onEdit.accept(p);
                    });
                    toggleBtn.setOnAction(e -> {
                        Product p = getTableView().getItems().get(getIndex());
                        if (onToggleStatus != null) onToggleStatus.accept(p.productID, p.status);
                    });
                    deleteBtn.setOnAction(e -> {
                        Product p = getTableView().getItems().get(getIndex());
                        if (onDelete != null) onDelete.accept(p.productID);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        // Fixed row height — prevents gaps
        productTable.setFixedCellSize(48);
        productTable.setStyle("-fx-fixed-cell-size: 48;");
    }

    public void setProducts(List<Product> products,
                            Consumer<Product> onEdit,
                            IntConsumer onDelete,
                            BiConsumer<Integer, String> onToggleStatus) {
        this.onEdit         = onEdit;
        this.onDelete       = onDelete;
        this.onToggleStatus = onToggleStatus;
        if (productTable != null)
            productTable.setItems(FXCollections.observableArrayList(products));
    }
}