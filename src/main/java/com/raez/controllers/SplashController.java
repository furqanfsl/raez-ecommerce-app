package com.raez.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashController implements Initializable {

    private static final String[] ZEAR = {"Z", "E", "A", "R"};
    private static final String[] RAEZ = {"R", "A", "E", "Z"};

    private static final String STYLE_WHITE =
        "-fx-font-size: 130; -fx-font-weight: bold; -fx-text-fill: white;"
        + "-fx-effect: dropshadow(gaussian, rgba(0,100,255,0.6), 20, 0.3, 0, 0);";
    private static final String STYLE_CYAN =
        "-fx-font-size: 130; -fx-font-weight: bold; -fx-text-fill: #7dd3fc;"
        + "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.85), 26, 0.4, 0, 0);";

    @FXML private StackPane splashRoot;
    @FXML private Label letter0, letter1, letter2, letter3;
    @FXML private Label subtitleLabel;
    @FXML private Label loadingLabel;

    private Label[] letters;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        letters = new Label[] { letter0, letter1, letter2, letter3 };
        for (int i = 0; i < letters.length; i++) {
            letters[i].setText(ZEAR[i]);
            letters[i].setStyle(STYLE_WHITE);
        }

        // Scattered start: each glyph offset, invisible (assembly drop-in)
        letter0.setTranslateX(-280);
        letter0.setTranslateY(40);
        letter0.setOpacity(0);

        letter1.setTranslateX(200);
        letter1.setTranslateY(-200);
        letter1.setOpacity(0);

        letter2.setTranslateX(-180);
        letter2.setTranslateY(200);
        letter2.setOpacity(0);

        letter3.setTranslateX(300);
        letter3.setTranslateY(-40);
        letter3.setOpacity(0);

        subtitleLabel.setOpacity(0);
    }

    public void play(Runnable onComplete) {
        ParallelTransition assemble = new ParallelTransition(
            glyphFlyIn(letter0, Duration.ZERO),
            glyphFlyIn(letter1, Duration.millis(100)),
            glyphFlyIn(letter2, Duration.millis(200)),
            glyphFlyIn(letter3, Duration.millis(300))
        );

        PauseTransition holdWrongBrand = new PauseTransition(Duration.millis(450));

        ParallelTransition roboticRename = new ParallelTransition(
            mechanicalFlipTo(letter0, RAEZ[0], Duration.ZERO),
            mechanicalFlipTo(letter1, RAEZ[1], Duration.millis(110)),
            mechanicalFlipTo(letter2, RAEZ[2], Duration.millis(220)),
            mechanicalFlipTo(letter3, RAEZ[3], Duration.millis(330))
        );

        PauseTransition settleLook = new PauseTransition(Duration.millis(120));
        settleLook.setOnFinished(e -> {
            for (Label L : letters) {
                L.setStyle(STYLE_WHITE);
            }
        });

        FadeTransition subFade = new FadeTransition(Duration.millis(450), subtitleLabel);
        subFade.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(550), splashRoot);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(ev -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        SequentialTransition seq = new SequentialTransition(
            assemble,
            new PauseTransition(Duration.millis(200)),
            holdWrongBrand,
            roboticRename,
            settleLook,
            new PauseTransition(Duration.millis(200)),
            subFade,
            new PauseTransition(Duration.millis(700)),
            fadeOut
        );
        seq.play();
    }

    private ParallelTransition glyphFlyIn(Label letter, Duration delay) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(780), letter);
        tt.setToX(0);
        tt.setToY(0);
        tt.setDelay(delay);
        tt.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(600), letter);
        ft.setToValue(1.0);
        ft.setDelay(delay);

        return new ParallelTransition(tt, ft);
    }

    /** Mechanical flip on X: edge-on, swap glyph, snap open — staggered across slots. */
    private Transition mechanicalFlipTo(Label letter, String newChar, Duration delay) {
        ScaleTransition close = new ScaleTransition(Duration.millis(150), letter);
        close.setToX(0.06);
        close.setDelay(delay);
        close.setInterpolator(Interpolator.EASE_IN);
        close.setOnFinished(e -> {
            letter.setText(newChar);
            letter.setStyle(STYLE_CYAN);
        });

        ScaleTransition open = new ScaleTransition(Duration.millis(190), letter);
        open.setFromX(0.06);
        open.setToX(1.0);
        open.setDelay(delay.add(Duration.millis(150)));
        open.setInterpolator(Interpolator.EASE_OUT);

        return new SequentialTransition(close, open);
    }

    /** Restore splash after a failed handoff so the scene is not stuck on an invisible root. */
    public void resetVisibility() {
        Platform.runLater(() -> splashRoot.setOpacity(1.0));
    }
}
