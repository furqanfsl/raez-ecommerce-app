package com.raez.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminLoginModalLauncher {
    private static final Logger log = LoggerFactory.getLogger(AdminLoginModalLauncher.class);


    public static void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                AdminLoginModalLauncher.class.getResource("/fxml/AdminLoginModal.fxml"));
            Parent root = loader.load();
            AdminLoginModalController controller = loader.getController();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            modal.setTitle("Admin Login");

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            modal.setScene(scene);

            controller.setup(modal::close);
            modal.showAndWait();
        } catch (Exception e) {
            log.error("{}", "AdminLoginModalLauncher error: " + e.getMessage());
            log.error("Error", e);
        }
    }
}
