package com.raez.model;

/**
 * Plain entry point that does NOT extend Application.
 * Bypasses the JavaFX module-path classloader restriction that causes
 * "module javafx.controls not found" when the launcher itself extends Application.
 */
public class MainLauncher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
