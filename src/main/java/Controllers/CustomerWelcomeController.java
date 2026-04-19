package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;

public class CustomerWelcomeController {

    @FXML
    private void handleCustomerLogin(ActionEvent event) throws Exception {
        navigate(event, "/fxml/CustomerLogin.fxml");
    }

    @FXML
    private void handleStaffLogin(ActionEvent event) throws Exception {
        navigate(event, "/fxml/CustomerStaffLogin.fxml");
    }

    @FXML
    private void handleSuperAdmin(ActionEvent event) throws Exception {
        navigate(event, "/fxml/CustomerSuperAdminLogin.fxml");
    }

    private void navigate(ActionEvent event, String fxmlPath) throws Exception {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
        stage.setScene(scene);
    }
}
