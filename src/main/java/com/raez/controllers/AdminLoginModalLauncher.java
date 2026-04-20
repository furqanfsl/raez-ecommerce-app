package com.raez.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AdminLoginModalLauncher {

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
            System.err.println("AdminLoginModalLauncher error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
