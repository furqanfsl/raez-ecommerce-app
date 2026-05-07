package com.raez.controllers;

import com.raez.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductLoginModalLauncher {
    private static final Logger log = LoggerFactory.getLogger(ProductLoginModalLauncher.class);


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
            log.error("{}", "ProductLoginModalLauncher error: " + e.getMessage());
            log.error("Error", e);
        }
    }
}
