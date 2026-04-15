package Controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DeleteConfirmDialogLauncher {

    public static void show(String productName, Runnable onConfirm) {
        try {
            FXMLLoader loader = new FXMLLoader(
                DeleteConfirmDialogLauncher.class.getResource("/fxml/DeleteConfirmDialog.fxml")
            );
            Parent root = loader.load();
            DeleteConfirmDialogController controller = loader.getController();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            modal.setTitle("Delete Product");

            StackPane backdrop = new StackPane(root);
            backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            backdrop.setAlignment(javafx.geometry.Pos.CENTER);

            Scene scene = new Scene(backdrop, Color.TRANSPARENT);
            modal.setScene(scene);

            controller.setup(productName, () -> {
                if (onConfirm != null) onConfirm.run();
                modal.close();
            }, modal::close);

            modal.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}