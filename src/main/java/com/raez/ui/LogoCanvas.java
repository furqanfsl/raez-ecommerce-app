package com.raez.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LogoCanvas {

    private static final String[] LETTERS   = {"R", "A", "E", "Z"};
    private static final Color    INK       = Color.web("#f5f5f5", 0.92);
    private static final Color    SWEEP     = Color.web("#e63946");
    private static final Color    STATUS_INK = Color.web("#f5f5f5", 0.55);

    private static final double   FONT_SIZE = 64;
    private static final double   SCAN_W    = 520;
    private static final double   SCAN_H    = 110;
    private static final double   SWEEP_W   = 360;

    private static final long     SCAN_NS   = 900_000_000L;  // 900ms
    private static final long     SWEEP_NS  = 400_000_000L;  // 400ms

    private final VBox      container;
    private final HBox      wordmark;
    private final Canvas    scanCanvas;
    private final Canvas    sweepCanvas;
    private final Rectangle clip;
    private final Text      status;

    public LogoCanvas() {
        Font font = Font.font("Segoe UI Light", FontWeight.LIGHT, FONT_SIZE);

        wordmark = new HBox(8);
        wordmark.setAlignment(Pos.CENTER);
        for (String ch : LETTERS) {
            Text t = new Text(ch);
            t.setFont(font);
            t.setFill(INK);
            wordmark.getChildren().add(t);
        }
        // Clip starts at height 0 — the scan-line grows it downward to reveal letters
        clip = new Rectangle(800, 0);
        wordmark.setClip(clip);

        scanCanvas = new Canvas(SCAN_W, SCAN_H);
        scanCanvas.setMouseTransparent(true);
        StackPane wordLayer = new StackPane(wordmark, scanCanvas);
        wordLayer.setAlignment(Pos.CENTER);

        sweepCanvas = new Canvas(SWEEP_W, 4);
        StackPane sweepHolder = new StackPane(sweepCanvas);
        sweepHolder.setPrefHeight(8);

        status = new Text("SYSTEMS  ONLINE   ·   v1.0");
        status.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        status.setFill(STATUS_INK);
        status.setOpacity(0);

        container = new VBox(14, wordLayer, sweepHolder, status);
        container.setAlignment(Pos.CENTER);
    }

    public Node getNode() {
        return container;
    }

    public void play(Runnable onFinished) {
        container.applyCss();
        container.layout();
        double measured = wordmark.getLayoutBounds().getHeight();
        final double H  = measured > 10 ? measured + 6 : 80;

        // Scan-line reveal: AnimationTimer drives clip growth + the moving red line per frame
        final GraphicsContext sg = scanCanvas.getGraphicsContext2D();
        sg.setStroke(SWEEP);
        sg.setLineWidth(2);
        final double scanCenterTop = (SCAN_H - H) / 2.0;

        final long t0 = System.nanoTime();
        AnimationTimer scan = new AnimationTimer() {
            @Override public void handle(long now) {
                double p = Math.min(1.0, (now - t0) / (double) SCAN_NS);
                clip.setHeight(p * H);

                double y = scanCenterTop + p * H;
                sg.clearRect(0, 0, SCAN_W, SCAN_H);
                sg.setGlobalAlpha(0.22);
                sg.strokeLine(0, y - 1, SCAN_W, y - 1);
                sg.strokeLine(0, y + 1, SCAN_W, y + 1);
                sg.setGlobalAlpha(1.0);
                sg.strokeLine(0, y, SCAN_W, y);

                if (p >= 1.0) {
                    stop();
                    sg.clearRect(0, 0, SCAN_W, SCAN_H);
                    playSweep(onFinished);
                }
            }
        };
        scan.start();
    }

    private void playSweep(Runnable onFinished) {
        final GraphicsContext gc = sweepCanvas.getGraphicsContext2D();
        final double w = sweepCanvas.getWidth();
        final double y = sweepCanvas.getHeight() / 2.0;
        gc.setStroke(SWEEP);
        gc.setLineWidth(2);

        final long startNs = System.nanoTime();
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double t = Math.min(1.0, (now - startNs) / (double) SWEEP_NS);
                gc.clearRect(0, 0, w, sweepCanvas.getHeight());
                // AnimationTimer used here for frame-accurate sweep; FadeTransition used below for declarative status fade-in
                gc.strokeLine(0, y, w * t, y);
                if (t >= 1.0) {
                    stop();
                    fadeStatus(onFinished);
                }
            }
        };
        timer.start();
    }

    private void fadeStatus(Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(280), status);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.millis(260));
            hold.setOnFinished(ev -> { if (onFinished != null) onFinished.run(); });
            hold.play();
        });
        fade.play();
    }
}
