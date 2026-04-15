package com.reaz.model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    private BorderPane root;

    @Override
    public void start(Stage stage) throws Exception {
        root = new BorderPane();

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("RAEZ - Robotics E-Commerce");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        showHomePage();
    }

    private void showHomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/homepage.fxml")
            );
            root.setCenter(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/AdminDashboard.fxml")
            );
            root.setCenter(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}