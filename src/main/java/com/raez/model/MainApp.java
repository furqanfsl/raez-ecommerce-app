package com.raez.model;

import com.raez.db.DBConnection;
import com.raez.ui.LauncherStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);


    @Override
    public void start(Stage stage) {
        DBConnection.getInstance();
        NavigationRouter.getInstance().init(stage);

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        stage.setTitle("RAEZ - Robotics E-Commerce");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());
        stage.setWidth(vb.getWidth());
        stage.setHeight(vb.getHeight());
        // Main stage is built but not shown until the launcher finishes.

        new LauncherStage().show(() -> Platform.runLater(() -> showMainScene(stage)));
    }

    private void showMainScene(Stage stage) {
        try {
            var url = getClass().getResource("/fxml/ProductHomepage.fxml");
            if (url == null) {
                throw new IllegalStateException("Missing resource /fxml/ProductHomepage.fxml");
            }
            Parent main = FXMLLoader.load(url);
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(main, vb.getWidth(), vb.getHeight());

            var adminCss = getClass().getResource("/css/admin-theme.css");
            if (adminCss != null) scene.getStylesheets().add(adminCss.toExternalForm());

            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            log.error("{}", "MainApp: failed to load ProductHomepage: " + e.getMessage());
            log.error("Error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("RAEZ");
            alert.setHeaderText("Could not open the storefront");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
