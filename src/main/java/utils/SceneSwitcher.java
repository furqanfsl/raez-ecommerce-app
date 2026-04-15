package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneSwitcher {

    public static void switchScene(String fxmlFile, Node node) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/views/" + fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) node.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}