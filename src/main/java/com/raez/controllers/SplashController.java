package com.raez.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashController implements Initializable {

    @FXML private StackPane splashRoot;
    @FXML private MediaView videoView;

    private MediaPlayer mediaPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // No pre-roll animation — video is the splash
    }

    public void play(Runnable onComplete) {
        if (videoView != null) {
            videoView.fitWidthProperty().bind(splashRoot.widthProperty());
            videoView.fitHeightProperty().bind(splashRoot.heightProperty());
        }

        boolean videoStarted = tryStartVideo(onComplete);

        // Safety: if the video failed to load, don't trap the user on a black screen
        if (!videoStarted && onComplete != null) {
            Platform.runLater(onComplete);
        }
    }

    private boolean tryStartVideo(Runnable onComplete) {
        try {
            URL videoUrl = getClass().getResource("/videos/video.mp4");
            if (videoUrl == null || videoView == null) return false;

            Media media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setMute(true);
            videoView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                FadeTransition fade = new FadeTransition(Duration.millis(450), splashRoot);
                fade.setToValue(0);
                fade.setOnFinished(e -> { if (onComplete != null) onComplete.run(); });
                fade.play();
            }));

            mediaPlayer.setOnError(() -> {
                System.err.println("SplashController: video error — " + mediaPlayer.getError());
                if (onComplete != null) Platform.runLater(onComplete);
            });

            mediaPlayer.play();
            return true;
        } catch (Exception e) {
            System.err.println("SplashController: video setup failed — " + e.getMessage());
            return false;
        }
    }

    public void resetVisibility() {
        Platform.runLater(() -> {
            splashRoot.setOpacity(1.0);
            if (mediaPlayer != null) {
                try { mediaPlayer.stop(); } catch (Exception ignored) {}
            }
        });
    }
}
