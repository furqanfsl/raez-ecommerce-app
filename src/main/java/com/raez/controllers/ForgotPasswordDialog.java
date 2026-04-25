package com.raez.controllers;

import com.raez.service.PasswordResetService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ForgotPasswordDialog {

    public static void show(Window owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Forgot Password");
        dialog.setResizable(false);

        // ── Step 1: enter email ──────────────────────────────────────────
        Label step1Title = new Label("Recover your password");
        step1Title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #0a1628;");

        Label step1Hint = new Label("Enter the email on your account. We'll send you a 6-digit recovery code.");
        step1Hint.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        step1Hint.setWrapText(true);

        TextField emailField = new TextField();
        emailField.setPromptText("your@email.com");
        emailField.setStyle(fieldStyle());

        Label step1Status = new Label("");
        step1Status.setWrapText(true);
        step1Status.setVisible(false);
        step1Status.setManaged(false);

        Button sendBtn = new Button("Send Recovery Code");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setStyle(primaryBtnStyle());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle(secondaryBtnStyle());
        cancelBtn.setOnAction(e -> dialog.close());

        VBox step1 = new VBox(12, step1Title, step1Hint, emailField, step1Status, sendBtn, cancelBtn);
        step1.setPadding(new Insets(28));
        step1.setAlignment(Pos.TOP_LEFT);
        step1.setStyle("-fx-background-color: white;");
        step1.setPrefWidth(440);

        // ── Step 2: enter code + new password ────────────────────────────
        Label step2Title = new Label("Enter your recovery code");
        step2Title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #0a1628;");

        Label step2Hint = new Label("Check your inbox for the 6-digit code we just sent, then set a new password.");
        step2Hint.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        step2Hint.setWrapText(true);

        TextField codeField = new TextField();
        codeField.setPromptText("6-digit code");
        codeField.setStyle(fieldStyle());

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("New password");
        newPassField.setStyle(fieldStyle());

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm new password");
        confirmPassField.setStyle(fieldStyle());

        Label step2Status = new Label("");
        step2Status.setWrapText(true);
        step2Status.setVisible(false);
        step2Status.setManaged(false);

        Button resetBtn = new Button("Reset Password");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setStyle(primaryBtnStyle());

        Button backBtn = new Button("← Back");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setStyle(secondaryBtnStyle());

        VBox step2 = new VBox(12, step2Title, step2Hint,
                              makeLabel("Recovery code"), codeField,
                              makeLabel("New password"), newPassField,
                              makeLabel("Confirm password"), confirmPassField,
                              step2Status, resetBtn, backBtn);
        step2.setPadding(new Insets(28));
        step2.setAlignment(Pos.TOP_LEFT);
        step2.setStyle("-fx-background-color: white;");
        step2.setPrefWidth(440);
        step2.setVisible(false);
        step2.setManaged(false);

        // Root swaps between step1 and step2
        VBox root = new VBox(step1, step2);
        root.setStyle("-fx-background-color: white;");

        // ── Send button: call startRecovery, then reveal step 2 ──────────
        sendBtn.setOnAction(e -> {
            String target = emailField.getText().trim();
            if (target.isEmpty()) {
                showStatus(step1Status, "Please enter your email.", true);
                return;
            }
            sendBtn.setDisable(true);
            sendBtn.setText("Sending…");
            new Thread(() -> {
                PasswordResetService.Result r = PasswordResetService.startRecovery(target);
                javafx.application.Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    sendBtn.setText("Send Recovery Code");
                    switch (r) {
                        case SENT, SMTP_DISABLED -> {
                            // Move to step 2 regardless (SMTP_DISABLED still stores code; dev testing)
                            step1.setVisible(false);
                            step1.setManaged(false);
                            step2.setVisible(true);
                            step2.setManaged(true);
                            dialog.sizeToScene();
                        }
                        case EMAIL_NOT_FOUND -> {
                            // Ambiguous message to avoid account enumeration
                            showStatus(step1Status, "If that email is registered, a code has been sent.", false);
                        }
                        default -> showStatus(step1Status, "Could not process recovery. Please try again later.", true);
                    }
                });
            }, "raez-password-reset").start();
        });

        // ── Reset button: verify code and update password ─────────────────
        resetBtn.setOnAction(e -> {
            String code    = codeField.getText().trim();
            String newPass = newPassField.getText();
            String confirm = confirmPassField.getText();

            if (code.isEmpty() || newPass.isEmpty()) {
                showStatus(step2Status, "Please fill in all fields.", true);
                return;
            }
            if (!newPass.equals(confirm)) {
                showStatus(step2Status, "Passwords do not match.", true);
                return;
            }
            if (newPass.length() < 6) {
                showStatus(step2Status, "Password must be at least 6 characters.", true);
                return;
            }

            resetBtn.setDisable(true);
            resetBtn.setText("Resetting…");
            String email = emailField.getText().trim();
            new Thread(() -> {
                PasswordResetService.ResetResult r = PasswordResetService.verifyAndReset(email, code, newPass);
                javafx.application.Platform.runLater(() -> {
                    resetBtn.setDisable(false);
                    resetBtn.setText("Reset Password");
                    switch (r) {
                        case SUCCESS -> {
                            showStatus(step2Status, "Password updated successfully! You can now log in.", false);
                            resetBtn.setDisable(true);
                            confirmPassField.setDisable(true);
                            newPassField.setDisable(true);
                            codeField.setDisable(true);
                        }
                        case INVALID_CODE -> showStatus(step2Status, "Invalid code. Check the email and try again.", true);
                        case EXPIRED      -> showStatus(step2Status, "This code has expired. Request a new one.", true);
                        case ALREADY_USED -> showStatus(step2Status, "This code has already been used.", true);
                        default           -> showStatus(step2Status, "Something went wrong. Please try again.", true);
                    }
                });
            }, "raez-password-reset").start();
        });

        backBtn.setOnAction(e -> {
            step2.setVisible(false);
            step2.setManaged(false);
            step1.setVisible(true);
            step1.setManaged(true);
            step2Status.setVisible(false);
            step2Status.setManaged(false);
            codeField.clear();
            newPassField.clear();
            confirmPassField.clear();
            dialog.sizeToScene();
        });

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    private static Label makeLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        VBox.setMargin(l, new Insets(4, 0, -4, 0));
        return l;
    }

    private static void showStatus(Label lbl, String msg, boolean isError) {
        lbl.setText(msg);
        lbl.setStyle(isError
            ? "-fx-text-fill: #dc2626; -fx-font-size: 12;"
            : "-fx-text-fill: #16a34a; -fx-font-size: 12;");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private static String fieldStyle() {
        return "-fx-font-size: 13; -fx-padding: 10 14 10 14;" +
               "-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;";
    }

    private static String primaryBtnStyle() {
        return "-fx-background-color: #0A1628; -fx-text-fill: white; -fx-font-weight: bold;" +
               "-fx-padding: 11 0 11 0; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13;";
    }

    private static String secondaryBtnStyle() {
        return "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-font-weight: bold;" +
               "-fx-padding: 10 0 10 0; -fx-background-radius: 8; -fx-cursor: hand;";
    }
}
