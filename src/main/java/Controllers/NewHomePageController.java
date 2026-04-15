package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class NewHomePageController implements Initializable {

    @FXML private ImageView heroImage;
    @FXML private StackPane heroPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLocalImage();
    }

    private void loadLocalImage() {
        try {
            URL imageUrl = getClass().getResource("/images/hero.png");
            if (imageUrl != null) {
                Image img = new Image(imageUrl.toExternalForm());
                heroImage.setImage(img);
                System.out.println("Hero image loaded from local resources.");
            } else {
                System.err.println("hero.png not found in /images/ — using background colour.");
            }
        } catch (Exception e) {
            System.err.println("Hero image load failed: " + e.getMessage());
        }
    }
}