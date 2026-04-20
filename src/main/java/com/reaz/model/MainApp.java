package com.reaz.model;

import com.reaz.db.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Open DB before any controller loads DAOs (fail fast if SQLite is unavailable)
        DBConnection.getInstance();

        // Register stage with router BEFORE loading any FXML
        NavigationRouter.getInstance().init(stage);

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/ProductHomepage.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);

        stage.setTitle("RAEZ - Robotics E-Commerce");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
