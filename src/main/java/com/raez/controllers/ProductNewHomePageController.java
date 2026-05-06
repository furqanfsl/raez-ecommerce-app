package com.raez.controllers;

import com.raez.model.NavigationRouter;
import com.raez.model.Product;
import com.raez.service.ProductService;
import com.raez.util.GlassPlaceholder;
import com.raez.util.ProductImageUtil;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductNewHomePageController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ProductNewHomePageController.class);


    @FXML private StackPane heroPane;
    @FXML private StackPane heroVideoContainer;
    @FXML private Pane      heroRobotPane;
    @FXML private VBox      heroTextBox;
    @FXML private VBox      stripsContainer;

    private final ProductService productService = new ProductService();

    // Strip filter state (applied across all four strips)
    private String filterSort  = "Newest";
    private String filterPrice = "All prices";
    private String filterQuery = "";

    // Every infinite animation created for the hero + strips is tracked here so
    // we can stop them on re-render / scene detach. Before this, each filter
    // change leaked 5 Timelines which accumulated until the pulse thread choked
    // and started throwing NPEs against detached ScrollPanes.
    private final List<javafx.animation.Animation> stripAnimations = new ArrayList<>();
    private final List<javafx.animation.Animation> heroAnimations  = new ArrayList<>();

    // Shared, bounded executor so filter changes don't spawn dozens of raw threads.
    private static final ExecutorService IMAGE_LOADER =
        Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "home-image-loader");
            t.setDaemon(true);
            return t;
        });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildCategoryStrips();
        Platform.runLater(() -> {
            setupRobotAnimation();
            playHeroEntrance();
        });
        // Stop every animation the moment the page leaves the scene, so navigating
        // away doesn't leave pulsers + auto-scrollers burning CPU in the background.
        if (heroPane != null) {
            heroPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) stopAllAnimations();
            });
        }
    }

    private void stopStripAnimations() {
        for (javafx.animation.Animation a : stripAnimations) a.stop();
        stripAnimations.clear();
    }

    private void stopAllAnimations() {
        stopStripAnimations();
        for (javafx.animation.Animation a : heroAnimations) a.stop();
        heroAnimations.clear();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero — animated robot built from JavaFX shapes
    // ═══════════════════════════════════════════════════════════════════

    private void setupRobotAnimation() {
        if (heroRobotPane == null) return;

        Group robot = buildRobotGroup();

        // Center horizontally and shift down slightly so antenna never crosses header bar.
        double paneW = heroRobotPane.getWidth() > 0 ? heroRobotPane.getWidth() : 560;
        robot.setLayoutX(paneW / 2.0);
        robot.setLayoutY(80);
        // GPU-friendly: rendering cache + auto-cache during transforms.
        robot.setCache(true);
        robot.setCacheHint(javafx.scene.CacheHint.SPEED);
        heroRobotPane.getChildren().add(robot);

        // Re-center if pane is resized
        heroRobotPane.widthProperty().addListener((obs, old, w) ->
            robot.setLayoutX(w.doubleValue() / 2.0));

        // 1. Floating — whole robot hovers up and down (5s, ease-in-out)
        TranslateTransition float_ = new TranslateTransition(Duration.seconds(5), robot);
        float_.setFromY(0); float_.setToY(-14);
        float_.setAutoReverse(true);
        float_.setCycleCount(Timeline.INDEFINITE);
        float_.setInterpolator(Interpolator.EASE_BOTH);
        float_.play();
        heroAnimations.add(float_);

        // 2. Glowing pulse — eyes share one Glow for perf, but we still pulse the intensity
        Glow eyeGlow = new Glow(0.6);
        Circle leftEye  = (Circle) robot.getProperties().get("leftEye");
        Circle rightEye = (Circle) robot.getProperties().get("rightEye");
        if (leftEye != null) leftEye.setEffect(eyeGlow);
        if (rightEye != null) rightEye.setEffect(eyeGlow);
        Timeline eyePulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(eyeGlow.levelProperty(), 0.4, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(2.25),
                new KeyValue(eyeGlow.levelProperty(), 1.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(4.5),
                new KeyValue(eyeGlow.levelProperty(), 0.4, Interpolator.EASE_BOTH))
        );
        eyePulse.setCycleCount(Timeline.INDEFINITE);
        eyePulse.play();
        heroAnimations.add(eyePulse);

        // 2b. Periodic blink — eyes scaleY 1 → 0.08 → 1 with a small gap.
        Circle leftPupil  = (Circle) robot.getProperties().get("leftPupil");
        Circle rightPupil = (Circle) robot.getProperties().get("rightPupil");
        if (leftEye != null && rightEye != null) {
            Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(leftEye.scaleYProperty(),  1.0, Interpolator.EASE_BOTH),
                    new KeyValue(rightEye.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(4.6),
                    new KeyValue(leftEye.scaleYProperty(),  1.0, Interpolator.EASE_BOTH),
                    new KeyValue(rightEye.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(4.78),
                    new KeyValue(leftEye.scaleYProperty(),  0.08, Interpolator.EASE_BOTH),
                    new KeyValue(rightEye.scaleYProperty(), 0.08, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(4.98),
                    new KeyValue(leftEye.scaleYProperty(),  1.0, Interpolator.EASE_BOTH),
                    new KeyValue(rightEye.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(6.0),
                    new KeyValue(leftEye.scaleYProperty(),  1.0),
                    new KeyValue(rightEye.scaleYProperty(), 1.0))
            );
            blink.setCycleCount(Timeline.INDEFINITE);
            blink.play();
            heroAnimations.add(blink);
        }

        // 3. Chest panel glow pulse — the central indicator (btn1) throbs cyan
        Circle chestPanel = (Circle) robot.getProperties().get("chestPanel");
        if (chestPanel != null) {
            Glow chestGlow = new Glow(0.6);
            chestPanel.setEffect(chestGlow);
            Timeline chestPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(chestGlow.levelProperty(), 0.3, Interpolator.EASE_BOTH),
                    new KeyValue(chestPanel.opacityProperty(), 0.7, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(2.5),
                    new KeyValue(chestGlow.levelProperty(), 0.95, Interpolator.EASE_BOTH),
                    new KeyValue(chestPanel.opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(5),
                    new KeyValue(chestGlow.levelProperty(), 0.3, Interpolator.EASE_BOTH),
                    new KeyValue(chestPanel.opacityProperty(), 0.7, Interpolator.EASE_BOTH))
            );
            chestPulse.setCycleCount(Timeline.INDEFINITE);
            chestPulse.play();
            heroAnimations.add(chestPulse);
        }

        // 4. Antenna tip pulse — glowing + scaling
        Circle antennaTip = (Circle) robot.getProperties().get("antennaTip");
        if (antennaTip != null) {
            ScaleTransition antPulse = new ScaleTransition(Duration.seconds(1.6), antennaTip);
            antPulse.setFromX(1.0); antPulse.setToX(1.6);
            antPulse.setFromY(1.0); antPulse.setToY(1.6);
            antPulse.setAutoReverse(true);
            antPulse.setCycleCount(Timeline.INDEFINITE);
            antPulse.setInterpolator(Interpolator.EASE_BOTH);
            antPulse.play();
            heroAnimations.add(antPulse);
        }

        // 4b. Antenna sway — gentle rotation about its base. Whole assembly rotates.
        Group antennaAssembly = (Group) robot.getProperties().get("antennaAssembly");
        if (antennaAssembly != null) {
            RotateTransition antSway = new RotateTransition(Duration.seconds(3.2), antennaAssembly);
            antSway.setFromAngle(-9); antSway.setToAngle(9);
            antSway.setAutoReverse(true);
            antSway.setCycleCount(Timeline.INDEFINITE);
            antSway.setInterpolator(Interpolator.EASE_BOTH);
            antSway.play();
            heroAnimations.add(antSway);
        }

        // 5. Arm swing — slowed and smoothed to feel intentional, not jittery
        Rectangle leftArm  = (Rectangle) robot.getProperties().get("leftArm");
        Rectangle rightArm = (Rectangle) robot.getProperties().get("rightArm");
        Circle leftHand   = (Circle) robot.getProperties().get("leftHand");
        Circle rightHand  = (Circle) robot.getProperties().get("rightHand");
        if (leftArm != null && rightArm != null) {
            leftArm.setRotate(0);
            rightArm.setRotate(0);
            RotateTransition leftSwing = new RotateTransition(Duration.seconds(4), leftArm);
            leftSwing.setFromAngle(-6); leftSwing.setToAngle(6);
            leftSwing.setAutoReverse(true); leftSwing.setCycleCount(Timeline.INDEFINITE);
            leftSwing.setInterpolator(Interpolator.EASE_BOTH);
            leftSwing.play();
            RotateTransition rightSwing = new RotateTransition(Duration.seconds(4), rightArm);
            rightSwing.setFromAngle(6); rightSwing.setToAngle(-6);
            rightSwing.setAutoReverse(true); rightSwing.setCycleCount(Timeline.INDEFINITE);
            rightSwing.setInterpolator(Interpolator.EASE_BOTH);
            rightSwing.play();
            heroAnimations.add(leftSwing);
            heroAnimations.add(rightSwing);
        }

        // 5b. Periodic wave — every ~9 seconds the right arm + hand swing way out
        // (a friendly "hi"), then settle back into the idle swing rhythm.
        Group rightArmAssembly = (Group) robot.getProperties().get("rightArmAssembly");
        if (rightArmAssembly != null) {
            // Pivot is the shoulder (rectangle's top-center). Group rotation defaults
            // to its own bounds center, which works since the hand sits below.
            Timeline waveLoop = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(rightArmAssembly.rotateProperty(), 0,   Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(8.0),
                    new KeyValue(rightArmAssembly.rotateProperty(), 0,   Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(8.6),
                    new KeyValue(rightArmAssembly.rotateProperty(), -42, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(9.0),
                    new KeyValue(rightArmAssembly.rotateProperty(), -28, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(9.4),
                    new KeyValue(rightArmAssembly.rotateProperty(), -42, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(9.8),
                    new KeyValue(rightArmAssembly.rotateProperty(), -28, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(10.4),
                    new KeyValue(rightArmAssembly.rotateProperty(), 0,   Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(13.0),
                    new KeyValue(rightArmAssembly.rotateProperty(), 0))
            );
            waveLoop.setCycleCount(Timeline.INDEFINITE);
            waveLoop.play();
            heroAnimations.add(waveLoop);
        }

        // 5c. Hand "fingers" wiggle on hover (subtle ScaleTransition, not infinite)
        if (leftHand != null && rightHand != null) {
            ScaleTransition handBob = new ScaleTransition(Duration.seconds(2.4), leftHand);
            handBob.setFromX(1.0); handBob.setToX(1.08);
            handBob.setFromY(1.0); handBob.setToY(1.08);
            handBob.setAutoReverse(true);
            handBob.setCycleCount(Timeline.INDEFINITE);
            handBob.setInterpolator(Interpolator.EASE_BOTH);
            handBob.play();
            heroAnimations.add(handBob);
        }

        // 6. Scan line — vertical sweep across the head visor
        Rectangle scanLine = (Rectangle) robot.getProperties().get("scanLine");
        if (scanLine != null) {
            Timeline scan = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(scanLine.translateYProperty(), -5,  Interpolator.LINEAR),
                    new KeyValue(scanLine.opacityProperty(),    0.0, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(0.8),
                    new KeyValue(scanLine.opacityProperty(),    0.85, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(2.4),
                    new KeyValue(scanLine.translateYProperty(), 70,  Interpolator.LINEAR),
                    new KeyValue(scanLine.opacityProperty(),    0.0, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(3.6),
                    new KeyValue(scanLine.translateYProperty(), 70))
            );
            scan.setCycleCount(Timeline.INDEFINITE);
            scan.play();
            heroAnimations.add(scan);
        }

        // 7. Reactive head + pupils — pointer drives both. Head tilts ±7°,
        //    pupils slide within the eye sockets, body parallax remains.
        Group  headAssembly = (Group)  robot.getProperties().get("headAssembly");
        heroRobotPane.setOnMouseMoved(e -> {
            double w = heroRobotPane.getWidth();
            double h = heroRobotPane.getHeight();
            if (w <= 0 || h <= 0) return;
            double nx = (e.getX() / w) - 0.5;   // -0.5 .. 0.5
            double ny = (e.getY() / h) - 0.5;

            // Body parallax (subtle)
            robot.setLayoutX((w / 2.0) + nx * 10.0);
            robot.setLayoutY(80 + ny * 6.0);

            // Head tilt
            if (headAssembly != null) {
                headAssembly.setRotate(nx * 7.0);
            }
            // Pupils track — clamp inside the eye
            if (leftPupil != null) {
                leftPupil.setTranslateX(nx * 6.0);
                leftPupil.setTranslateY(ny * 4.0);
            }
            if (rightPupil != null) {
                rightPupil.setTranslateX(nx * 6.0);
                rightPupil.setTranslateY(ny * 4.0);
            }
        });
        heroRobotPane.setOnMouseExited(e -> {
            robot.setLayoutX(heroRobotPane.getWidth() / 2.0);
            robot.setLayoutY(80);
            if (headAssembly != null) headAssembly.setRotate(0);
            if (leftPupil  != null) { leftPupil.setTranslateX(0);  leftPupil.setTranslateY(0); }
            if (rightPupil != null) { rightPupil.setTranslateX(0); rightPupil.setTranslateY(0); }
        });

        // 8. Click anywhere on the robot — quick "happy spin" on the antenna tip
        //    plus a chest pulse. Gives the user feedback they hit something.
        heroRobotPane.setOnMouseClicked(e -> {
            if (antennaTip != null) {
                ScaleTransition burst = new ScaleTransition(Duration.millis(180), antennaTip);
                burst.setFromX(antennaTip.getScaleX()); burst.setFromY(antennaTip.getScaleY());
                burst.setToX(2.4); burst.setToY(2.4);
                burst.setAutoReverse(true); burst.setCycleCount(2);
                burst.play();
            }
            if (chestPanel != null) {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(200), chestPanel);
                pulse.setFromX(1.0); pulse.setToX(1.5);
                pulse.setFromY(1.0); pulse.setToY(1.5);
                pulse.setAutoReverse(true); pulse.setCycleCount(2);
                pulse.play();
            }
        });
    }

    private Group buildRobotGroup() {
        Group g = new Group();
        Color accentColor = Color.web("#00e5ff");
        Color accent2     = Color.web("#5eead4");
        Color accent3     = Color.web("#38bdf8");
        double stroke     = 1.8;

        // Brushed gradient fills give every metal panel a soft top-down highlight
        // so the robot stops looking like flat 2D rectangles in the dark hero.
        LinearGradient bodyFill = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#1c2942")),
            new Stop(0.5, Color.web("#0e1729")),
            new Stop(1, Color.web("#070c18")));
        LinearGradient limbFill = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#16223a")),
            new Stop(1, Color.web("#070c18")));
        RadialGradient visorFill = new RadialGradient(
            0, 0, 0.5, 0.4, 0.7, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0d1c33")),
            new Stop(1, Color.web("#04090f")));

        // Soft ambient drop-shadow tying the robot to the gradient mesh behind it.
        DropShadow ambientShadow = new DropShadow(28, 0, 14, Color.color(0, 0, 0, 0.55));
        DropShadow accentGlow    = new DropShadow(18, Color.color(0, 0.9, 1, 0.45));
        accentGlow.setSpread(0.05);

        // ── Antenna assembly (rotates as one unit) ─────────────────────────
        Line antennaLine = new Line(0, -45, 0, -5);
        antennaLine.setStroke(accentColor); antennaLine.setStrokeWidth(2.5);
        Circle antennaTip = new Circle(0, -52, 9);
        antennaTip.setFill(accentColor);
        antennaTip.setEffect(new Glow(1.0));
        Group antennaAssembly = new Group(antennaLine, antennaTip);
        // Translate origin so rotation pivots from base of antenna (y=-5)
        antennaAssembly.setTranslateY(-5);
        antennaLine.setStartY(antennaLine.getStartY() + 5);
        antennaLine.setEndY(antennaLine.getEndY() + 5);
        antennaTip.setCenterY(antennaTip.getCenterY() + 5);

        // ── Head assembly: visor frame + visor glass + eyes + pupils + mouth + scan line
        Rectangle headFrame = new Rectangle(-62, -7, 124, 86);
        headFrame.setArcWidth(24); headFrame.setArcHeight(24);
        headFrame.setFill(bodyFill);
        headFrame.setStroke(accentColor); headFrame.setStrokeWidth(stroke);
        headFrame.setEffect(accentGlow);

        // Tinted visor glass — sits inside the frame, eyes float over it
        Rectangle visor = new Rectangle(-50, 8, 100, 50);
        visor.setArcWidth(18); visor.setArcHeight(18);
        visor.setFill(visorFill);
        visor.setStroke(Color.color(0, 0.9, 1, 0.35));
        visor.setStrokeWidth(0.8);

        // Cheek vents (small detail panels on each side of head)
        Rectangle leftVent  = new Rectangle(-58, 30, 4, 18);
        leftVent.setArcWidth(2); leftVent.setArcHeight(2);
        leftVent.setFill(accent3); leftVent.setOpacity(0.55);
        Rectangle rightVent = new Rectangle(54, 30, 4, 18);
        rightVent.setArcWidth(2); rightVent.setArcHeight(2);
        rightVent.setFill(accent3); rightVent.setOpacity(0.55);

        // Eyes (slightly larger, over the visor)
        Circle leftEye = new Circle(-22, 34, 14);
        leftEye.setFill(accentColor);
        Circle rightEye = new Circle(22, 34, 14);
        rightEye.setFill(accentColor);

        // Pupils — black dot inside, will track the cursor
        Circle leftPupil = new Circle(-22, 34, 6);
        leftPupil.setFill(Color.web("#001a26"));
        Circle rightPupil = new Circle(22, 34, 6);
        rightPupil.setFill(Color.web("#001a26"));

        // Eye highlight (small white spec for life)
        Circle leftEyeShine  = new Circle(-25, 31, 2.4);
        leftEyeShine.setFill(Color.color(1, 1, 1, 0.85));
        leftEyeShine.setMouseTransparent(true);
        Circle rightEyeShine = new Circle(19, 31, 2.4);
        rightEyeShine.setFill(Color.color(1, 1, 1, 0.85));
        rightEyeShine.setMouseTransparent(true);

        // Scan line — thin cyan bar that sweeps down the visor
        Rectangle scanLine = new Rectangle(-50, 8, 100, 1.5);
        scanLine.setFill(Color.color(0, 0.9, 1, 0.85));
        scanLine.setEffect(new Glow(0.85));
        scanLine.setMouseTransparent(true);
        scanLine.setOpacity(0.0);

        // Mouth bar — segmented LED strip
        Rectangle mouth = new Rectangle(-33, 60, 66, 8);
        mouth.setArcWidth(8); mouth.setArcHeight(8);
        mouth.setFill(accentColor); mouth.setOpacity(0.55);

        Group headAssembly = new Group(
            headFrame, visor, scanLine,
            leftVent, rightVent,
            leftEye, rightEye, leftPupil, rightPupil,
            leftEyeShine, rightEyeShine,
            mouth
        );

        // ── Neck ───────────────────────────────────────────────────────────
        Rectangle neck = new Rectangle(-16, 79, 32, 22);
        neck.setFill(limbFill); neck.setStroke(accentColor); neck.setStrokeWidth(1.2);

        // ── Body / chest ───────────────────────────────────────────────────
        Rectangle body = new Rectangle(-82, 101, 164, 158);
        body.setArcWidth(20); body.setArcHeight(20);
        body.setFill(bodyFill); body.setStroke(accentColor); body.setStrokeWidth(stroke);
        body.setEffect(ambientShadow);

        // Chest plate seam line (vertical)
        Line chestSeam = new Line(0, 110, 0, 250);
        chestSeam.setStroke(Color.color(0, 0.9, 1, 0.18));
        chestSeam.setStrokeWidth(0.8);

        // Chest indicator cluster
        Circle btn1 = new Circle(-30, 155, 9);
        btn1.setFill(accentColor); btn1.setEffect(new Glow(0.6));
        Circle btn2 = new Circle(0, 155, 9);
        btn2.setFill(accent2); btn2.setOpacity(0.75);
        Circle btn3 = new Circle(30, 155, 9);
        btn3.setFill(accent3); btn3.setOpacity(0.55);

        // Panel bars
        Rectangle panel1 = new Rectangle(-52, 180, 104, 5);
        panel1.setArcWidth(5); panel1.setArcHeight(5);
        panel1.setFill(accentColor); panel1.setOpacity(0.4);
        Rectangle panel2 = new Rectangle(-52, 196, 68, 5);
        panel2.setArcWidth(5); panel2.setArcHeight(5);
        panel2.setFill(accentColor); panel2.setOpacity(0.22);
        Rectangle panel3 = new Rectangle(-52, 212, 90, 3);
        panel3.setArcWidth(3); panel3.setArcHeight(3);
        panel3.setFill(accent3); panel3.setOpacity(0.18);

        // ── Arms — wrap each in a Group so the wave timeline can rotate the
        //          whole arm + hand from the shoulder.
        Rectangle leftArm = new Rectangle(-116, 110, 34, 105);
        leftArm.setArcWidth(14); leftArm.setArcHeight(14);
        leftArm.setFill(limbFill); leftArm.setStroke(accentColor); leftArm.setStrokeWidth(stroke);
        Circle leftHand = new Circle(-99, 228, 16);
        leftHand.setFill(limbFill); leftHand.setStroke(accent2); leftHand.setStrokeWidth(stroke);
        Group leftArmAssembly = new Group(leftArm, leftHand);

        Rectangle rightArm = new Rectangle(82, 110, 34, 105);
        rightArm.setArcWidth(14); rightArm.setArcHeight(14);
        rightArm.setFill(limbFill); rightArm.setStroke(accentColor); rightArm.setStrokeWidth(stroke);
        Circle rightHand = new Circle(99, 228, 16);
        rightHand.setFill(limbFill); rightHand.setStroke(accent2); rightHand.setStrokeWidth(stroke);
        Group rightArmAssembly = new Group(rightArm, rightHand);

        // ── Legs / feet ────────────────────────────────────────────────────
        Rectangle leftLeg = new Rectangle(-70, 254, 44, 98);
        leftLeg.setArcWidth(14); leftLeg.setArcHeight(14);
        leftLeg.setFill(limbFill); leftLeg.setStroke(accentColor); leftLeg.setStrokeWidth(stroke);
        Rectangle rightLeg = new Rectangle(26, 254, 44, 98);
        rightLeg.setArcWidth(14); rightLeg.setArcHeight(14);
        rightLeg.setFill(limbFill); rightLeg.setStroke(accentColor); rightLeg.setStrokeWidth(stroke);

        Rectangle leftFoot = new Rectangle(-80, 343, 62, 23);
        leftFoot.setArcWidth(12); leftFoot.setArcHeight(12);
        leftFoot.setFill(limbFill); leftFoot.setStroke(accentColor); leftFoot.setStrokeWidth(1.2);
        Rectangle rightFoot = new Rectangle(18, 343, 62, 23);
        rightFoot.setArcWidth(12); rightFoot.setArcHeight(12);
        rightFoot.setFill(limbFill); rightFoot.setStroke(accentColor); rightFoot.setStrokeWidth(1.2);

        // Subtle ground halo — radial glow under the feet
        Circle groundGlow = new Circle(0, 380, 90);
        groundGlow.setFill(new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(0, 0.9, 1, 0.30)),
            new Stop(1, Color.TRANSPARENT)));
        groundGlow.setMouseTransparent(true);

        g.getChildren().addAll(
            groundGlow,
            leftArmAssembly, rightArmAssembly,
            leftLeg, rightLeg, leftFoot, rightFoot,
            neck, body, chestSeam,
            btn1, btn2, btn3, panel1, panel2, panel3,
            antennaAssembly,
            headAssembly
        );

        // Property bag for animation lookup
        g.getProperties().put("leftEye",         leftEye);
        g.getProperties().put("rightEye",        rightEye);
        g.getProperties().put("leftPupil",       leftPupil);
        g.getProperties().put("rightPupil",      rightPupil);
        g.getProperties().put("antennaTip",      antennaTip);
        g.getProperties().put("antennaAssembly", antennaAssembly);
        g.getProperties().put("leftArm",         leftArm);
        g.getProperties().put("rightArm",        rightArm);
        g.getProperties().put("leftHand",        leftHand);
        g.getProperties().put("rightHand",       rightHand);
        g.getProperties().put("rightArmAssembly", rightArmAssembly);
        g.getProperties().put("leftArmAssembly",  leftArmAssembly);
        g.getProperties().put("chestPanel",      btn1);
        g.getProperties().put("scanLine",        scanLine);
        g.getProperties().put("headAssembly",    headAssembly);
        return g;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero entrance animation — text slides in from left, video from right
    // ═══════════════════════════════════════════════════════════════════

    private void playHeroEntrance() {
        if (heroTextBox == null) return;

        heroTextBox.setOpacity(0);
        heroTextBox.setTranslateX(-30);

        FadeTransition textFade = new FadeTransition(Duration.millis(800), heroTextBox);
        textFade.setFromValue(0); textFade.setToValue(1);
        TranslateTransition textSlide = new TranslateTransition(Duration.millis(800), heroTextBox);
        textSlide.setFromX(-30); textSlide.setToX(0);
        textSlide.setInterpolator(Interpolator.EASE_OUT);

        if (heroVideoContainer != null) {
            heroVideoContainer.setOpacity(0);
            heroVideoContainer.setTranslateX(30);
            FadeTransition robotFade = new FadeTransition(Duration.millis(900), heroVideoContainer);
            robotFade.setFromValue(0); robotFade.setToValue(1);
            TranslateTransition robotSlide = new TranslateTransition(Duration.millis(900), heroVideoContainer);
            robotSlide.setFromX(30); robotSlide.setToX(0);
            robotSlide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(textFade, textSlide, robotFade, robotSlide).play();
        } else {
            new ParallelTransition(textFade, textSlide).play();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Horizontal category strips
    // ═══════════════════════════════════════════════════════════════════

    private void buildCategoryStrips() {
        if (stripsContainer == null) return;
        stripsContainer.getChildren().clear();

        stripsContainer.getChildren().add(buildFilterBar());
        renderStrips();
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding: 4 0 18 0;");

        Label title = new Label("// BROWSE CATALOG");
        title.setStyle(
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #00d4ff;" +
            "-fx-font-family: 'Courier New';"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Search products…");
        search.setPrefWidth(200);
        search.setStyle("-fx-font-size: 12; -fx-padding: 6 10; -fx-background-radius: 6;" +
                        "-fx-border-color: #e5e7eb; -fx-border-radius: 6;");
        search.textProperty().addListener((o, a, b) -> {
            filterQuery = b == null ? "" : b.trim();
            renderStrips();
        });

        ComboBox<String> price = new ComboBox<>();
        price.getItems().addAll("All prices", "Under £200", "£200–£400", "£400–£600", "Over £600");
        price.setValue(filterPrice);
        price.setStyle("-fx-font-size: 12;");
        price.setOnAction(e -> { filterPrice = price.getValue(); renderStrips(); });

        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("Newest", "Price: Low to High", "Price: High to Low", "Name: A–Z");
        sort.setValue(filterSort);
        sort.setStyle("-fx-font-size: 12;");
        sort.setOnAction(e -> { filterSort = sort.getValue(); renderStrips(); });

        Label priceLbl = new Label("Price:");
        priceLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        Label sortLbl = new Label("Sort:");
        sortLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");

        bar.getChildren().addAll(title, spacer, search, priceLbl, price, sortLbl, sort);
        return bar;
    }

    private void renderStrips() {
        if (stripsContainer == null) return;
        // Stop every auto-scroll Timeline from the previous render before
        // discarding the nodes — otherwise each filter change doubles the
        // number of live Timelines, which eventually starves the JavaFX pulse
        // and surfaces as NPEs against detached ScrollPanes.
        stopStripAnimations();
        if (stripsContainer.getChildren().size() > 1) {
            stripsContainer.getChildren().remove(1, stripsContainer.getChildren().size());
        }

        List<Product> all;
        try {
            all = productService.getActive();
        } catch (Exception e) {
            log.error("{}", "ProductNewHomePageController: product load failed: " + e.getMessage());
            return;
        }

        stripsContainer.getChildren().add(buildStrip(
            "The Apex Series", "Aetherion Prime · Aura-Scout V1 · Nexus-Node · Pulse-Crawler",
            filterByCollection(all, "The Apex Series"), "the-apex-series"));

        stripsContainer.getChildren().add(buildStrip(
            "The Ledger Series", "Ledger-Bot X9 · Quant-Mite · Ticker-Drone · DataVault Crawler",
            filterByCollection(all, "The Ledger Series"), "the-ledger-series"));

        stripsContainer.getChildren().add(buildStrip(
            "The Velocity Series", "Veloce-Mach 1 · Aero-Slip · G-Force Tracker · Apex-Rover",
            filterByCollection(all, "The Velocity Series"), "the-velocity-series"));

        stripsContainer.getChildren().add(buildStrip(
            "The Sentinel Series", "Aegis-Warden · Sonar-Bat · Thermo-Gnat · Volt-Tick",
            filterByCollection(all, "The Sentinel Series"), "the-sentinel-series"));

        List<Product> standalone = filterByCollection(all, null);
        if (!standalone.isEmpty()) {
            stripsContainer.getChildren().add(buildStrip(
                "Standalone Units", "Cipher-Sentinel · Echo-Medic · Solara-Harvest & more",
                standalone, null));
        }
    }

    private List<Product> filterByCollection(List<Product> all, String collectionName) {
        double min = 0, max = Double.MAX_VALUE;
        switch (filterPrice) {
            case "Under £200" -> { min = 0;   max = 200; }
            case "£200–£400" -> { min = 200; max = 400; }
            case "£400–£600" -> { min = 400; max = 600; }
            case "Over £600" -> { min = 600; max = Double.MAX_VALUE; }
            default -> { /* All prices */ }
        }
        final double fMin = min, fMax = max;
        final String q = filterQuery == null ? "" : filterQuery.toLowerCase();

        List<Product> matches = all.stream()
            .filter(p -> collectionName == null
                ? (p.collection == null || p.collection.isBlank())
                : collectionName.equals(p.collection))
            .filter(p -> p.price >= fMin && p.price <= fMax)
            .filter(p -> q.isEmpty()
                || (p.name != null && p.name.toLowerCase().contains(q))
                || (p.description != null && p.description.toLowerCase().contains(q)))
            .collect(Collectors.toList());

        Comparator<Product> cmp = switch (filterSort) {
            case "Price: Low to High"  -> Comparator.comparingDouble((Product p) -> p.price);
            case "Price: High to Low"  -> Comparator.comparingDouble((Product p) -> p.price).reversed();
            case "Name: A–Z"           -> Comparator.comparing(p -> p.name == null ? "" : p.name.toLowerCase());
            default                    -> Comparator.comparingInt((Product p) -> p.productID).reversed();
        };
        matches.sort(cmp);

        if (matches.size() > 12) matches = new ArrayList<>(matches.subList(0, 12));
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Apex Series — Bento Grid layout
    // Aetherion Prime: 2×2 feature tile. Others: 1×1 tiles.
    // ═══════════════════════════════════════════════════════════════════

    private VBox buildApexBento(List<Product> products) {
        VBox section = new VBox(16);
        section.setStyle("-fx-padding: 22 0 26 0;");
        section.setMaxWidth(Double.MAX_VALUE);

        // Header row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label eyebrow = new Label("FEATURED COLLECTION");
        eyebrow.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #5eead4;" +
            "-fx-letter-spacing: 3;"
        );
        Label titleLbl = new Label("The Apex Series");
        titleLbl.setStyle(
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 30; -fx-font-weight: 900; -fx-text-fill: white;"
        );
        Label subLbl = new Label("Flagship bipedal orchestration. Pick your platform.");
        subLbl.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 13; -fx-text-fill: #cbd5e1;"
        );
        titleBox.getChildren().addAll(eyebrow, titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Paging arrows + Explore button.
        Button prevBtn = buildBentoPagerButton("‹");
        Button nextBtn = buildBentoPagerButton("›");
        Button viewAll = new Button("Explore →");
        viewAll.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 32; -fx-border-width: 0.5;" +
            "-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-cursor: hand; -fx-padding: 9 20; -fx-background-radius: 32;"
        );
        viewAll.setOnAction(e ->
            NavigationRouter.getInstance().navigateByPath("/collections/the-apex-series"));

        header.getChildren().addAll(titleBox, spacer, prevBtn, nextBtn, viewAll);
        section.getChildren().add(header);

        if (products.isEmpty()) {
            section.getChildren().add(buildComingSoonCard("The Apex Series"));
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            return section;
        }

        // Pick feature product (prefer Aetherion Prime, fall back to first)
        final Product feature = products.stream()
            .filter(p -> p.name != null && p.name.toLowerCase().contains("aetherion"))
            .findFirst()
            .orElse(products.get(0));

        // All non-feature products form the paginated pool.
        final List<Product> pool = new ArrayList<>(products);
        pool.remove(feature);

        final int tilesPerPage = 4;
        final int pageCount = pool.isEmpty() ? 1 : (int) Math.ceil(pool.size() / (double) tilesPerPage);

        // Grid: 4 columns × 2 rows. Feature spans (col 0-1, row 0-1). Small tiles fill col 2-3.
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setMaxWidth(Double.MAX_VALUE);

        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 2; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(220);
            rc.setMinHeight(200);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        // Feature tile (2×2) — stays put across pages.
        VBox featureTile = buildBentoFeatureTile(feature);
        grid.add(featureTile, 0, 0, 2, 2);
        GridPane.setHgrow(featureTile, Priority.ALWAYS);
        GridPane.setVgrow(featureTile, Priority.ALWAYS);

        final int[] cols = { 2, 3, 2, 3 };
        final int[] rows = { 0, 0, 1, 1 };
        final int[] pageIndex = { 0 };

        Runnable renderPage = () -> {
            // Remove existing small tiles (everything except the feature at 0,0 span 2×2).
            grid.getChildren().removeIf(node -> node != featureTile);

            int start = pageIndex[0] * tilesPerPage;
            for (int i = 0; i < tilesPerPage; i++) {
                int idx = start + i;
                if (idx >= pool.size()) break;
                StackPane tile = buildBentoSmallTile(pool.get(idx));
                grid.add(tile, cols[i], rows[i]);
                GridPane.setHgrow(tile, Priority.ALWAYS);
                GridPane.setVgrow(tile, Priority.ALWAYS);
            }
            prevBtn.setDisable(pageIndex[0] == 0);
            nextBtn.setDisable(pageIndex[0] >= pageCount - 1);
        };

        prevBtn.setOnAction(e -> {
            if (pageIndex[0] > 0) { pageIndex[0]--; renderPage.run(); }
        });
        nextBtn.setOnAction(e -> {
            if (pageIndex[0] < pageCount - 1) { pageIndex[0]++; renderPage.run(); }
        });

        renderPage.run();
        section.getChildren().add(grid);
        return section;
    }

    private Button buildBentoPagerButton(String glyph) {
        Button b = new Button(glyph);
        b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 50; -fx-border-width: 0.5;" +
            "-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-cursor: hand;" +
            "-fx-min-width: 38; -fx-min-height: 38;" +
            "-fx-max-width: 38; -fx-max-height: 38;" +
            "-fx-padding: 0 0 3 0;"
        );
        return b;
    }

    private VBox buildBentoFeatureTile(Product product) {
        VBox tile = new VBox(0);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setMaxHeight(Double.MAX_VALUE);
        tile.setStyle(
            "-fx-background-color: rgba(13,17,30,0.75);" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 24; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 24, 0.3, 0, 10);"
        );

        // Image pane fills available space above info strip
        StackPane imagePane = buildProductImagePane(product, 600, 300, 24);
        imagePane.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(imagePane, Priority.ALWAYS);
        tile.getChildren().add(imagePane);

        // Info strip at the bottom (same style as mini card)
        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 16 20 18 20;");
        Label eyebrow = new Label("FEATURED");
        eyebrow.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #5eead4; -fx-letter-spacing: 3;"
        );
        Label name = new Label(product.name);
        name.setStyle(
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 20; -fx-font-weight: 900; -fx-text-fill: white;"
        );
        name.setWrapText(true);
        Label price = new Label(String.format("£%,.0f", product.price));
        price.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #38bdf8;"
        );
        info.getChildren().addAll(eyebrow, name, price);
        tile.getChildren().add(info);

        tile.setOnMouseEntered(e -> tile.setStyle(
            "-fx-background-color: rgba(30,38,58,0.85);" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(94,234,212,0.55); -fx-border-radius: 24; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(56,189,248,0.35), 22, 0.45, 0, 0);"
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
            "-fx-background-color: rgba(13,17,30,0.75);" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 24; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 24, 0.3, 0, 10);"
        ));
        tile.setOnMouseClicked(e -> NavigationRouter.getInstance().navigateToProductDetail(product));
        return tile;
    }

    private StackPane buildBentoSmallTile(Product product) {
        StackPane tile = new StackPane();
        tile.setStyle(
            "-fx-background-color: rgba(13,17,30,0.75);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 20; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 14, 0.25, 0, 6);"
        );

        // Generative backdrop fills whole tile
        StackPane backdrop = buildGenerativePlaceholder(product.name, 20);
        backdrop.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0a0f1f 0%, #121a2e 50%, #1a2744 100%);" +
            "-fx-background-radius: 20;"
        );
        tile.getChildren().add(backdrop);

        ImageView iv = new ImageView();
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setVisible(false);
        iv.fitWidthProperty().bind(tile.widthProperty());
        iv.fitHeightProperty().bind(tile.heightProperty());
        Rectangle clip = new Rectangle();
        clip.setArcWidth(40); clip.setArcHeight(40);
        clip.widthProperty().bind(tile.widthProperty());
        clip.heightProperty().bind(tile.heightProperty());
        iv.setClip(clip);
        tile.getChildren().add(iv);

        String primary = product.getPrimaryImage();
        if (primary != null && !primary.isEmpty()) {
            IMAGE_LOADER.submit(() -> {
                Image img = ProductImageUtil.loadThumbnail(primary, 300, 300);
                if (img != null && !img.isError()) {
                    Platform.runLater(() -> {
                        iv.setImage(img);
                        iv.setVisible(true);
                    });
                }
            });
        }

        // Bottom-gradient legibility overlay
        Pane overlay = new Pane();
        overlay.setStyle(
            "-fx-background-color: linear-gradient(to top, rgba(5,10,22,0.82) 0%, transparent 60%);" +
            "-fx-background-radius: 20;"
        );
        overlay.setMouseTransparent(true);
        tile.getChildren().add(overlay);

        // Copy
        VBox copy = new VBox(3);
        copy.setStyle("-fx-padding: 16;");
        StackPane.setAlignment(copy, Pos.BOTTOM_LEFT);
        Label name = new Label(product.name);
        name.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;"
        );
        name.setWrapText(true);
        name.setMaxWidth(220);
        Label price = new Label(String.format("£%,.0f", product.price));
        price.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #38bdf8;"
        );
        copy.getChildren().addAll(name, price);
        tile.getChildren().add(copy);

        // Hover CTA
        Label viewBadge = new Label("View →");
        viewBadge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #0a0f1f;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-padding: 7 14; -fx-background-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0.2, 0, 2);"
        );
        viewBadge.setOpacity(0);
        viewBadge.setMouseTransparent(true);
        StackPane.setAlignment(viewBadge, Pos.CENTER);
        tile.getChildren().add(viewBadge);

        attachBentoHover(tile, viewBadge, false);
        tile.setOnMouseClicked(e -> NavigationRouter.getInstance().navigateToProductDetail(product));
        return tile;
    }

    private void attachBentoHover(StackPane tile, Label viewBadge, boolean isFeature) {
        // Tile chrome stays fixed — the only hover feedback is the "View Details"
        // badge fading in. No size, position, border, or shadow changes so tiles
        // never appear to "zoom" as the pointer crosses them.
        tile.setOnMouseEntered(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), viewBadge);
            ft.setToValue(1); ft.play();
        });
        tile.setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), viewBadge);
            ft.setToValue(0); ft.play();
        });
    }

    private VBox buildStrip(String title, String subtitle, List<Product> products, String collectionSlug) {
        VBox strip = new VBox(10);
        strip.setStyle("-fx-padding: 18 0 18 0;");

        // Header row
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #00d4ff;");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewAll = new Button("Explore →");
        viewAll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: #00d4ff; -fx-border-radius: 4; -fx-border-width: 1;" +
            "-fx-text-fill: #00d4ff; -fx-font-size: 12; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 6 14;"
        );
        if (collectionSlug != null) {
            viewAll.setOnAction(e -> NavigationRouter.getInstance().navigateByPath("/collections/" + collectionSlug));
        } else {
            viewAll.setOnAction(e -> {
                ProductListController.setPendingCategory("Main Robot");
                navigateToHome();
            });
        }

        header.getChildren().addAll(titleBox, spacer, viewAll);
        strip.getChildren().add(header);

        // Cards row — horizontal
        HBox cardRow = new HBox(14);
        cardRow.setStyle("-fx-padding: 6 2 12 2;");

        if (products.isEmpty()) {
            cardRow.getChildren().add(buildComingSoonCard(title));
        } else {
            for (Product p : products) cardRow.getChildren().add(buildMiniCard(p));
        }

        // Wrap in a ScrollPane with scrollbars hidden via CSS.
        // Height = card body (160 image + 110 info) + drop shadow margin.
        // setFitToHeight(false) so cards keep their natural size; the strip
        // grows to match the card rather than cropping the price + badge.
        ScrollPane scroll = new ScrollPane(cardRow);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                        "-fx-background: transparent;");
        scroll.setPrefHeight(312);
        scroll.setMinHeight(312);

        // Overlay arrows (circular, half-transparent)
        Button leftBtn  = buildNavArrow("‹");
        Button rightBtn = buildNavArrow("›");

        StackPane viewport = new StackPane(scroll, leftBtn, rightBtn);
        StackPane.setAlignment(leftBtn,  Pos.CENTER_LEFT);
        StackPane.setAlignment(rightBtn, Pos.CENTER_RIGHT);
        leftBtn.setTranslateX(8);
        rightBtn.setTranslateX(-8);

        // Auto-scroll: sweep hvalue 0 → 1 and back, continuously, with ease-in-out (slow drift)
        Timeline autoScroll = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(scroll.hvalueProperty(), 0.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(38),
                new KeyValue(scroll.hvalueProperty(), 1.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(76),
                new KeyValue(scroll.hvalueProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        autoScroll.setCycleCount(Timeline.INDEFINITE);
        stripAnimations.add(autoScroll);
        // Start after layout so hvalue is meaningful
        Platform.runLater(autoScroll::play);

        // Pause on hover; resume on exit
        viewport.setOnMouseEntered(e -> autoScroll.pause());
        viewport.setOnMouseExited (e -> autoScroll.play());

        // Manual step
        leftBtn.setOnAction(e -> {
            autoScroll.pause();
            scroll.setHvalue(Math.max(0.0, scroll.getHvalue() - 0.2));
        });
        rightBtn.setOnAction(e -> {
            autoScroll.pause();
            scroll.setHvalue(Math.min(1.0, scroll.getHvalue() + 0.2));
        });

        scroll.prefViewportWidthProperty().bind(stripsContainer.widthProperty());
        scroll.setMaxWidth(Double.MAX_VALUE);
        viewport.setMaxWidth(Double.MAX_VALUE);
        strip.setMaxWidth(Double.MAX_VALUE);

        strip.getChildren().add(viewport);
        return strip;
    }

    private Button buildNavArrow(String glyph) {
        Button b = new Button(glyph);
        b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92);" +
            "-fx-text-fill: #1E2939; -fx-font-size: 22; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-border-color: transparent;" +
            "-fx-min-width: 40; -fx-min-height: 40;" +
            "-fx-max-width: 40; -fx-max-height: 40;" +
            "-fx-padding: 0 0 3 0; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 2);"
        );
        return b;
    }

    private VBox buildMiniCard(Product product) {
        VBox card = new VBox(0);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setMinWidth(220);
        // Lock card height so the strip's no-internal-scroll viewport always
        // shows the full image + price without cropping the bottom row.
        card.setPrefHeight(280);
        card.setMinHeight(280);
        card.setMaxHeight(280);
        card.setStyle(
            "-fx-background-color: rgba(13,17,30,0.75);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 20; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.25, 0, 6);"
        );

        // Image area: generative placeholder + async real image
        StackPane imagePane = buildProductImagePane(product, 220, 160, 20);

        // Info area — glass style
        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 12 14 14 14;");
        Label name = new Label(product.name);
        name.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;"
        );
        name.setWrapText(true);
        name.setMaxWidth(200);

        // Category badge
        String catName = (product.categories != null && !product.categories.isEmpty()
                          && product.categories.get(0).categoryName != null)
            ? product.categories.get(0).categoryName : "";
        Label catBadge = new Label(catName.toUpperCase());
        catBadge.setStyle(
            "-fx-font-size: 9; -fx-text-fill: #5eead4;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif; -fx-font-weight: bold;" +
            "-fx-letter-spacing: 2;"
        );

        Label price = new Label(String.format("£%,.0f", product.price));
        price.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 15; -fx-text-fill: #38bdf8; -fx-font-weight: bold;"
        );
        info.getChildren().addAll(catBadge, name, price);

        card.getChildren().addAll(imagePane, info);

        // Hover — glass brightens + "View Details" chip appears
        Label viewBadge = new Label("View Details →");
        viewBadge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #0a0f1f;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 11; -fx-font-weight: bold;" +
            "-fx-padding: 7 14; -fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.2, 0, 3);"
        );
        viewBadge.setOpacity(0);
        viewBadge.setMouseTransparent(true);
        StackPane.setAlignment(viewBadge, Pos.CENTER);
        imagePane.getChildren().add(viewBadge);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: rgba(30,38,58,0.85);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(94,234,212,0.55); -fx-border-radius: 20; -fx-border-width: 0.5;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(56,189,248,0.35), 22, 0.45, 0, 0);"
            );
            FadeTransition ft = new FadeTransition(Duration.millis(180), viewBadge);
            ft.setToValue(1); ft.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: rgba(13,17,30,0.75);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 20; -fx-border-width: 0.5;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.25, 0, 6);"
            );
            FadeTransition ft = new FadeTransition(Duration.millis(180), viewBadge);
            ft.setToValue(0); ft.play();
        });
        card.setOnMouseClicked(e -> NavigationRouter.getInstance().navigateToProductDetail(product));
        return card;
    }

    /** Thin adapter — delegates to the shared {@link GlassPlaceholder} util. */
    private StackPane buildGenerativePlaceholder(String productName, int arcRadius) {
        return GlassPlaceholder.buildTopRounded(productName, arcRadius, 42);
    }

    /**
     * Image pane that starts with a generative initials placeholder, then
     * swaps in the real image once loaded from disk/network.
     */
    private StackPane buildProductImagePane(Product product, int width, int height, int arcRadius) {
        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(width, height);
        imagePane.setMinHeight(height);
        imagePane.setMaxHeight(height);

        StackPane placeholder = buildGenerativePlaceholder(product.name, arcRadius);
        placeholder.prefWidthProperty().bind(imagePane.widthProperty());
        placeholder.prefHeightProperty().bind(imagePane.heightProperty());
        imagePane.getChildren().add(placeholder);

        ImageView iv = new ImageView();
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setVisible(false);

        // Clip image corners to top of card
        Rectangle imgClip = new Rectangle(width, height);
        imgClip.setArcWidth(arcRadius);
        imgClip.setArcHeight(arcRadius);
        iv.setClip(imgClip);
        imagePane.getChildren().add(iv);

        String primary = product.getPrimaryImage();
        if (primary != null && !primary.isEmpty()) {
            IMAGE_LOADER.submit(() -> {
                Image img = ProductImageUtil.loadThumbnail(primary, 300, 300);
                if (img != null && !img.isError()) {
                    Platform.runLater(() -> {
                        iv.setImage(img);
                        iv.setVisible(true);
                        placeholder.setVisible(false);
                    });
                }
            });
        }

        return imagePane;
    }

    private VBox buildComingSoonCard(String collectionName) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setMinHeight(220);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #090d1a;" +
            "-fx-background-radius: 8; -fx-border-color: #1a3a6e;" +
            "-fx-border-radius: 8; -fx-border-width: 1; -fx-border-style: dashed;"
        );
        Label code = new Label("// ASSETS PENDING");
        code.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11; -fx-text-fill: #2a4a7a;");
        Label title = new Label(collectionName);
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
        Label body = new Label("Deploying this range shortly.");
        body.setStyle("-fx-font-size: 11; -fx-text-fill: #3a5c8a;");
        card.getChildren().addAll(code, title, body);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Hero button handlers & navigation
    // ═══════════════════════════════════════════════════════════════════

    @FXML
    private void handleExploreCollection() {
        NavigationRouter.getInstance().navigateByPath("/collections/the-apex-series");
    }

    @FXML
    private void handleShopNow() {
        navigateToHome();
    }

    private void navigateToHome() {
        try {
            javafx.scene.Parent view = FXMLLoader.load(
                getClass().getResource("/fxml/ProductListPage.fxml"));
            heroPane.getScene().setRoot(view);
        } catch (Exception e) {
            log.error("{}", "ProductNewHomePageController: nav failed: " + e.getMessage());
        }
    }
}
