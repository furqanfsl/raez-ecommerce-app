package com.raez.controllers;

import com.raez.model.Product;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductFormDialogLauncher {
    private static final Logger log = LoggerFactory.getLogger(ProductFormDialogLauncher.class);


    public static void show(Product product, Consumer<Product> onSubmit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                ProductFormDialogLauncher.class.getResource("/fxml/ProductFormDialog.fxml")
            );
            Parent root = loader.load();
            ProductFormDialogController controller = loader.getController();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.DECORATED);
            modal.setTitle(product != null ? "Edit Product" : "Add Product");
            modal.setResizable(false);

            Scene scene = new Scene(root, 600, 700);
            modal.setScene(scene);

            controller.setup(product, onSubmit, modal::close);

            modal.showAndWait();

        } catch (Exception e) {
            log.error("{}", "ProductFormDialogLauncher error: " + e.getMessage());
            log.error("Error", e);
        }
    }
}