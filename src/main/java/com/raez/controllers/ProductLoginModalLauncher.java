package com.raez.controllers;

import com.raez.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class ProductLoginModalLauncher {

    public static void show(Consumer<User> onLoginSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(
                ProductLoginModalLauncher.class.getResource("/fxml/ProductLoginModal.fxml")
            );
            Parent root = loader.load();
            ProductLoginModalController controller = loader.getController();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            modal.setTitle("Login");

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            modal.setScene(scene);

            controller.setup(onLoginSuccess, modal::close);

            modal.showAndWait();

        } catch (Exception e) {
            System.err.println("ProductLoginModalLauncher error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
