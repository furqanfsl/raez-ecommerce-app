package Controllers;

import com.reaz.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class LoginModalLauncher {

    public static void show(Consumer<User> onLoginSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(
                LoginModalLauncher.class.getResource("/fxml/LoginModal.fxml")
            );
            Parent root = loader.load();
            LoginModalController controller = loader.getController();

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
            System.err.println("LoginModalLauncher error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}