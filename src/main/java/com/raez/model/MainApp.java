package com.raez.model;

import com.raez.controllers.SplashController;
import com.raez.db.DBConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DBConnection.getInstance();
        NavigationRouter.getInstance().init(stage);

        FXMLLoader splashLoader = new FXMLLoader(
            getClass().getResource("/fxml/Splash.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashController splashCtrl = splashLoader.getController();

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(splashRoot, vb.getWidth(), vb.getHeight());

        // Attach the admin-theme stylesheet globally so every admin FXML
        // can use the shared .admin-* classes without needing a per-view link.
        var adminCss = getClass().getResource("/css/admin-theme.css");
        if (adminCss != null) scene.getStylesheets().add(adminCss.toExternalForm());

        stage.setTitle("RAEZ - Robotics E-Commerce");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());
        stage.setWidth(vb.getWidth());
        stage.setHeight(vb.getHeight());
        stage.show();

        splashCtrl.play(() -> Platform.runLater(() -> {
            try {
                var url = getClass().getResource("/fxml/ProductHomepage.fxml");
                if (url == null) {
                    throw new IllegalStateException("Missing resource /fxml/ProductHomepage.fxml");
                }
                Parent main = FXMLLoader.load(url);
                stage.getScene().setRoot(main);
            } catch (Exception e) {
                System.err.println("MainApp: failed to load ProductHomepage: " + e.getMessage());
                e.printStackTrace();
                splashCtrl.resetVisibility();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("RAEZ");
                alert.setHeaderText("Could not open the storefront");
                alert.setContentText(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                alert.showAndWait();
            }
        }));
    }

    public static void main(String[] args) {
        launch();
    }
}
