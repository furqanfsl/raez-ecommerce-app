package com.raez.controllers;

import com.raez.model.Product;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class ProductFormDialogLauncher {

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
            System.err.println("ProductFormDialogLauncher error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}