package com.raez.warehouse.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.Optional;

/**
 * DialogUtil — reusable dialog and label helper methods.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles dialog/alert UI helpers
 *  - DRY: removes duplicated showError/hideError/confirm/info from every controller
 */
public class Warehouse_DialogUtil {

    // Prevent instantiation
    private Warehouse_DialogUtil() {}

    /**
     * Shows a confirmation dialog. Returns true if user clicked OK.
     */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shows an information dialog.
     */
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error dialog.
     */
    public static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error message on a Label.
     */
    public static void showError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    /**
     * Hides an error Label.
     */
    public static void hideError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }
}