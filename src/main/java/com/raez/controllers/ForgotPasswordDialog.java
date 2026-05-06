package com.raez.controllers;

import com.raez.service.PasswordResetService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Two-stage password recovery flow:
 *  Stage 1 — enter email, receive 6-digit code (token valid 7 minutes)
 *  Stage 2 — enter code + new password, with 60-second resend cooldown
 */
public class ForgotPasswordDialog {

    public static void show(Window owner) {
        showEmailStage(owner, "");
    }

    // ── Stage 1: enter email ──────────────────────────────────────────────

    private static void showEmailStage(Window owner, String prefillEmail) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Recover password");
        dialog.setResizable(false);

        // Background layers
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        Pane bg1 = pane("linear-gradient(to bottom right, #0a0f1f 0%, #0d2538 35%, #1a3347 60%, #2a1a4a 100%)");
        Pane bg2 = pane("radial-gradient(center 15% 20%, radius 55%, rgba(20,184,166,0.22) 0%, transparent 70%)");
        Pane bg3 = pane("radial-gradient(center 80% 80%, radius 55%, rgba(139,92,246,0.22) 0%, transparent 70%)");
        bg1.setMinSize(460, 360); bg2.setMinSize(460, 360); bg3.setMinSize(460, 360);

        // Glass card
        StackPane card = new StackPane();
        card.setStyle(
            "-fx-background-color: rgba(13,21,32,0.88);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;");
        DropShadow ds = new DropShadow(30, 0, 8, Color.rgb(0, 0, 0, 0.60));
        card.setEffect(ds);

        VBox body = new VBox(14);
        body.setPadding(new Insets(30, 32, 30, 32));
        body.setMinWidth(396);
        body.setMaxWidth(396);

        Label eyebrow = label("ACCOUNT RECOVERY",
            "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #5eead4; -fx-letter-spacing: 3;");
        Label title = label("Recover your password",
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 20; -fx-font-weight: 900; -fx-text-fill: white;");
        Label hint = label("Enter the email address on your account.\nWe'll send a 6-digit code (valid 7 minutes).",
            "-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.60);");
        hint.setWrapText(true);

        Label emailLbl = label("EMAIL ADDRESS",
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.65); -fx-letter-spacing: 2;");
        TextField emailField = darkField("your@email.com");
        if (!prefillEmail.isBlank()) emailField.setText(prefillEmail);

        Label status = label("",
            "-fx-font-size: 12; -fx-text-fill: #fca5a5;");
        status.setWrapText(true);
        status.setVisible(false);
        status.setManaged(false);

        Button sendBtn = primaryBtn("Send Recovery Code");
        Button cancelBtn = ghostBtn("Cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        sendBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) { showStatus(status, "Please enter your email.", true); return; }
            sendBtn.setDisable(true);
            sendBtn.setText("Sending…");
            status.setVisible(false); status.setManaged(false);
            new Thread(() -> {
                PasswordResetService.Result r = PasswordResetService.startRecovery(email);
                // Fetch fallback code on background thread (DB access — keep off FX thread)
                String fallbackCode = (r == PasswordResetService.Result.SMTP_DISABLED)
                    ? PasswordResetService.getLatestPendingCode(email) : null;
                javafx.application.Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    sendBtn.setText("Send Recovery Code");
                    switch (r) {
                        case SENT -> {
                            dialog.close();
                            showResetStage(owner, email, null);
                        }
                        case SMTP_DISABLED -> {
                            dialog.close();
                            showResetStage(owner, email, fallbackCode);
                        }
                        case EMAIL_NOT_FOUND ->
                            // Deliberately ambiguous — anti-enumeration
                            showStatus(status, "If that email is registered, a code has been sent.", false);
                        default ->
                            showStatus(status, "Could not start recovery. Please try again.", true);
                    }
                });
            }, "raez-reset-send").start();
        });

        body.getChildren().addAll(eyebrow, title, hint, emailLbl, emailField, status, sendBtn, cancelBtn);
        card.getChildren().add(body);
        root.getChildren().addAll(bg1, bg2, bg3, card);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Stage 2: enter code + new password ───────────────────────────────

    private static void showResetStage(Window owner, String email, String smtpFallbackCode) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Reset password");
        dialog.setResizable(false);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        Pane bg1 = pane("linear-gradient(to bottom right, #0a0f1f 0%, #0d2538 35%, #1a3347 60%, #2a1a4a 100%)");
        Pane bg2 = pane("radial-gradient(center 15% 20%, radius 55%, rgba(20,184,166,0.22) 0%, transparent 70%)");
        Pane bg3 = pane("radial-gradient(center 80% 80%, radius 55%, rgba(139,92,246,0.22) 0%, transparent 70%)");
        bg1.setMinSize(460, 560); bg2.setMinSize(460, 560); bg3.setMinSize(460, 560);

        StackPane card = new StackPane();
        card.setStyle(
            "-fx-background-color: rgba(13,21,32,0.88);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;");
        DropShadow ds = new DropShadow(30, 0, 8, Color.rgb(0, 0, 0, 0.60));
        card.setEffect(ds);

        VBox body = new VBox(14);
        body.setPadding(new Insets(30, 32, 30, 32));
        body.setMinWidth(396);
        body.setMaxWidth(396);

        Label eyebrow = label("RESET PASSWORD",
            "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #5eead4; -fx-letter-spacing: 3;");
        Label title = label("Enter your recovery code",
            "-fx-font-family: 'Inter','Segoe UI','Montserrat',sans-serif;" +
            "-fx-font-size: 20; -fx-font-weight: 900; -fx-text-fill: white;");
        boolean smtpOff = smtpFallbackCode != null;
        Label hint = label(smtpOff
            ? "SMTP is not configured — your code is shown below. Set up SMTP in Super Admin settings to receive codes by email."
            : "Check your inbox for the 6-digit code we just sent to " + email + ".",
            "-fx-font-size: 12; -fx-text-fill: " + (smtpOff ? "rgba(251,191,36,0.90)" : "rgba(255,255,255,0.60)") + ";");
        hint.setWrapText(true);

        // SMTP-off banner — shows the code prominently so the flow works without email
        if (smtpOff) {
            StackPane codeBanner = new StackPane();
            codeBanner.setStyle(
                "-fx-background-color: rgba(251,191,36,0.12);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(251,191,36,0.35);" +
                "-fx-border-radius: 12; -fx-border-width: 0.5;" +
                "-fx-padding: 14 18;");
            Label codeDisplay = label(smtpFallbackCode,
                "-fx-font-family: 'Courier New','Consolas',monospace;" +
                "-fx-font-size: 32; -fx-font-weight: bold; -fx-letter-spacing: 8;" +
                "-fx-text-fill: #fbbf24;" +
                "-fx-effect: dropshadow(gaussian, rgba(251,191,36,0.55), 10, 0.25, 0, 0);");
            Label codeNote = label("⚠  Demo mode — SMTP disabled",
                "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: rgba(251,191,36,0.70);" +
                "-fx-letter-spacing: 1;");
            VBox bannerBody = new VBox(4, codeDisplay, codeNote);
            bannerBody.setAlignment(javafx.geometry.Pos.CENTER);
            codeBanner.getChildren().add(bannerBody);
            body.getChildren().add(codeBanner);
        }

        // Code row: field + resend button with 60-second cooldown
        Label codeLbl = label("RECOVERY CODE",
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.65); -fx-letter-spacing: 2;");
        TextField codeField = darkField("6-digit code");
        codeField.setStyle(codeField.getStyle() +
            "-fx-font-size: 18; -fx-font-weight: bold; -fx-letter-spacing: 4;");
        if (smtpFallbackCode != null) codeField.setText(smtpFallbackCode);

        Button resendBtn = new Button("Resend code");
        resendBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #5eead4;" +
            "-fx-font-size: 11; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-underline: true;" +
            "-fx-padding: 0; -fx-border-color: transparent;");

        // 60-second cooldown timeline
        int[] countdown = {60};
        Timeline[] cooldown = {null};
        Runnable startCooldown = () -> {
            resendBtn.setDisable(true);
            countdown[0] = 60;
            resendBtn.setText("Resend code (" + countdown[0] + "s)");
            cooldown[0] = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                countdown[0]--;
                if (countdown[0] <= 0) {
                    resendBtn.setDisable(false);
                    resendBtn.setText("Resend code");
                    if (cooldown[0] != null) cooldown[0].stop();
                } else {
                    resendBtn.setText("Resend code (" + countdown[0] + "s)");
                }
            }));
            cooldown[0].setCycleCount(60);
            cooldown[0].play();
        };
        // Start cooldown immediately (code was just sent)
        javafx.application.Platform.runLater(startCooldown::run);

        Label newPassLbl = label("NEW PASSWORD",
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.65); -fx-letter-spacing: 2;");
        PasswordField newPassField = darkPass("New password (min. 6 chars)");

        Label confirmPassLbl = label("CONFIRM PASSWORD",
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: rgba(255,255,255,0.65); -fx-letter-spacing: 2;");
        PasswordField confirmPassField = darkPass("Confirm new password");

        Label status = label("",
            "-fx-font-size: 12; -fx-text-fill: #fca5a5;");
        status.setWrapText(true);
        status.setVisible(false);
        status.setManaged(false);

        Button resetBtn = primaryBtn("Reset Password");
        Button backBtn  = ghostBtn("← Back to email entry");

        resendBtn.setOnAction(e -> {
            resendBtn.setDisable(true);
            new Thread(() -> {
                PasswordResetService.Result r = PasswordResetService.startRecovery(email);
                String newCode = (r == PasswordResetService.Result.SMTP_DISABLED)
                    ? PasswordResetService.getLatestPendingCode(email) : null;
                javafx.application.Platform.runLater(() -> {
                    boolean ok = r == PasswordResetService.Result.SENT
                              || r == PasswordResetService.Result.SMTP_DISABLED;
                    if (ok && newCode != null) {
                        // Update code field with the fresh code
                        codeField.setText(newCode);
                        showStatus(status, "New code ready (SMTP off — shown above).", false);
                    } else {
                        showStatus(status, ok
                            ? "A new code has been sent to " + email + "."
                            : "Could not send code. Please wait and try again.", !ok);
                    }
                    startCooldown.run();
                });
            }, "raez-reset-resend").start();
        });

        resetBtn.setOnAction(e -> {
            String code    = codeField.getText().trim();
            String newPass = newPassField.getText();
            String confirm = confirmPassField.getText();
            if (code.isEmpty() || newPass.isEmpty()) {
                showStatus(status, "Please fill in all fields.", true); return;
            }
            if (!newPass.equals(confirm)) {
                showStatus(status, "Passwords do not match.", true); return;
            }
            if (newPass.length() < 6) {
                showStatus(status, "Password must be at least 6 characters.", true); return;
            }
            resetBtn.setDisable(true);
            resetBtn.setText("Resetting…");
            status.setVisible(false); status.setManaged(false);
            new Thread(() -> {
                PasswordResetService.ResetResult r =
                    PasswordResetService.verifyAndReset(email, code, newPass);
                javafx.application.Platform.runLater(() -> {
                    resetBtn.setDisable(false);
                    resetBtn.setText("Reset Password");
                    switch (r) {
                        case SUCCESS -> {
                            if (cooldown[0] != null) cooldown[0].stop();
                            showStatus(status,
                                "Password updated! You can now log in.", false);
                            status.setStyle(
                                "-fx-font-size: 12; -fx-text-fill: #5eead4;");
                            resetBtn.setDisable(true);
                            codeField.setDisable(true);
                            newPassField.setDisable(true);
                            confirmPassField.setDisable(true);
                            resendBtn.setDisable(true);
                        }
                        case INVALID_CODE ->
                            showStatus(status, "Invalid code — check your email and try again.", true);
                        case EXPIRED ->
                            showStatus(status, "This code has expired. Use the resend button to get a new one.", true);
                        case ALREADY_USED ->
                            showStatus(status, "This code has already been used.", true);
                        default ->
                            showStatus(status, "Something went wrong. Please try again.", true);
                    }
                });
            }, "raez-reset-verify").start();
        });

        backBtn.setOnAction(e -> {
            if (cooldown[0] != null) cooldown[0].stop();
            dialog.close();
            showEmailStage(owner, email);
        });

        HBox resendRow = new HBox(resendBtn);
        resendRow.setAlignment(Pos.CENTER_RIGHT);

        body.getChildren().addAll(eyebrow, title, hint,
            codeLbl, codeField, resendRow,
            newPassLbl, newPassField,
            confirmPassLbl, confirmPassField,
            status, resetBtn, backBtn);
        card.getChildren().add(body);
        root.getChildren().addAll(bg1, bg2, bg3, card);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static Label label(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    private static TextField darkField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(
            "-fx-font-size: 13; -fx-padding: 11 14 11 14;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.35);" +
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 12; -fx-border-width: 0.5;");
        return f;
    }

    private static PasswordField darkPass(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle(
            "-fx-font-size: 13; -fx-padding: 11 14 11 14;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.35);" +
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 12; -fx-border-width: 0.5;");
        return f;
    }

    private static Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-text-fill: #0a0f1f;" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-padding: 12 0 12 0; -fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.6);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(94,234,212,0.40), 12, 0.2, 0, 3);");
        return b;
    }

    private static Button ghostBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: rgba(255,255,255,0.80);" +
            "-fx-font-family: 'Inter','Segoe UI',sans-serif;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-padding: 11 0 11 0; -fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 22; -fx-border-width: 0.5;" +
            "-fx-cursor: hand;");
        return b;
    }

    private static Pane pane(String bgStyle) {
        Pane p = new Pane();
        p.setStyle("-fx-background-color: " + bgStyle + ";");
        p.setMouseTransparent(true);
        return p;
    }

    private static void showStatus(Label lbl, String msg, boolean isError) {
        lbl.setText(msg);
        lbl.setStyle(isError
            ? "-fx-text-fill: #fca5a5; -fx-font-size: 12;"
            : "-fx-text-fill: #5eead4; -fx-font-size: 12;");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
}
