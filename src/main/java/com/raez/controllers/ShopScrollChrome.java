package com.raez.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;

/**
 * Shared "scroll to top" FAB behaviour for shop FXML layouts.
 */
public final class ShopScrollChrome {

    private ShopScrollChrome() {}

    public static void wire(ScrollPane scrollPane, Button scrollToTopBtn) {
        if (scrollPane == null || scrollToTopBtn == null) {
            return;
        }
        scrollToTopBtn.setVisible(false);
        scrollToTopBtn.setManaged(false);

        scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            boolean show = v > 0.1;
            scrollToTopBtn.setVisible(show);
            scrollToTopBtn.setManaged(show);
        });

        scrollToTopBtn.setOnAction(e -> {
            double start = scrollPane.getVvalue();
            if (start <= 0.01) {
                scrollPane.setVvalue(0.0);
                return;
            }
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scrollPane.vvalueProperty(), start)),
                new KeyFrame(Duration.millis(300), new KeyValue(scrollPane.vvalueProperty(), 0.0))
            );
            timeline.play();
        });
    }
}
