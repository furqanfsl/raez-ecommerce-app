package com.raez.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LauncherStage {

    private static final double WIDTH  = 600;
    private static final double HEIGHT = 400;

    private final Stage stage;
    private final StackPane root;

    public LauncherStage() {
        this.root = new StackPane();
        this.root.getStyleClass().add("launcher-card");

        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.TRANSPARENT);
        var css = getClass().getResource("/styles/launcher.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        this.stage = new Stage(StageStyle.TRANSPARENT);
        this.stage.setScene(scene);
        this.stage.setResizable(false);
        this.stage.setAlwaysOnTop(true);

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        this.stage.setX(vb.getMinX() + (vb.getWidth()  - WIDTH)  / 2);
        this.stage.setY(vb.getMinY() + (vb.getHeight() - HEIGHT) / 2);
    }

    public void show(Runnable onFinished) {
        LogoCanvas logo = new LogoCanvas();
        root.getChildren().setAll(logo.getNode());
        stage.show();
        logo.play(() -> {
            stage.close();
            if (onFinished != null) onFinished.run();
        });
    }
}
