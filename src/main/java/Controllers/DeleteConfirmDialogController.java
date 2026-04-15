package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DeleteConfirmDialogController implements Initializable {

    @FXML private Label descriptionLabel;

    private Runnable onConfirm;
    private Runnable onCancel;

    public void setup(String productName, Runnable onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel  = onCancel;
        descriptionLabel.setText(
            "Are you sure you want to delete \"" + productName + "\"? This action cannot be undone. " +
            "This will permanently remove the product from the system and may affect references " +
            "in orders, reviews, and inventory records."
        );
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML
    private void handleConfirm() {
        if (onConfirm != null) onConfirm.run();
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) onCancel.run();
    }
}