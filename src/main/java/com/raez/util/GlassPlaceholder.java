package com.raez.util;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Dark glass placeholder with glowing product initials.
 *
 * Used wherever a Product has no real image — keeps placeholders visually
 * consistent with the rest of the Liquid Glass aesthetic and stops legacy
 * stock photos or oversized SVG icons bleeding through.
 */
public final class GlassPlaceholder {

    private GlassPlaceholder() {}

    /** Default corner radius (matches card chrome). */
    public static StackPane build(String productName) {
        return build(productName, 20, 42);
    }

    /** Full control: specify corner radius and initial font size. */
    public static StackPane build(String productName, double arcRadius, double fontSize) {
        StackPane pane = new StackPane();
        pane.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0a0f1f 0%, #121a2e 45%, #1a2744 100%);" +
            "-fx-background-radius: " + arcRadius + ";"
        );

        Pane glow = new Pane();
        glow.setStyle(
            "-fx-background-color: radial-gradient(center 50% 50%, radius 55%, " +
            "rgba(56,189,248,0.18) 0%, transparent 70%);"
        );
        glow.setMouseTransparent(true);
        pane.getChildren().add(glow);

        Label initials = new Label(extractInitials(productName));
        initials.setStyle(
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: " + fontSize + ";" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: linear-gradient(to bottom right, #5eead4, #38bdf8 60%, #8b5cf6);" +
            "-fx-effect: dropshadow(gaussian, rgba(56,189,248,0.55), 18, 0.35, 0, 0);" +
            "-fx-letter-spacing: 2;"
        );
        pane.getChildren().add(initials);

        return pane;
    }

    /** Top-corner-only rounded variant for cards that stack info below the image. */
    public static StackPane buildTopRounded(String productName, double arcRadius, double fontSize) {
        StackPane pane = build(productName, arcRadius, fontSize);
        pane.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0a0f1f 0%, #121a2e 45%, #1a2744 100%);" +
            "-fx-background-radius: " + arcRadius + " " + arcRadius + " 0 0;"
        );
        return pane;
    }

    /**
     * "Aetherion Prime" → "AP", "Veloce-Mach 1" → "VM1", "Red Dead" → "RD".
     */
    public static String extractInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        StringBuilder sb = new StringBuilder();
        String[] tokens = name.trim().split("[\\s\\-_/]+");
        for (String tok : tokens) {
            if (tok.isEmpty()) continue;
            char c = tok.charAt(0);
            if (Character.isLetterOrDigit(c)) sb.append(Character.toUpperCase(c));
            if (sb.length() >= 3) break;
        }
        if (sb.length() == 0) sb.append("R");
        return sb.toString();
    }
}
