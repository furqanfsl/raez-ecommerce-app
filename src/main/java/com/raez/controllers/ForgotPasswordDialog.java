package com.raez.controllers;

import com.raez.service.PasswordResetService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

        Label title = new Label("Recover your password");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #000080;");

        Label hint = new Label("Enter the email on your account. We'll send a 6-digit recovery code.");
        hint.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        hint.setWrapText(true);

        TextField email = new TextField();
        email.setPromptText("your@email.com");
        email.setStyle("-fx-font-size: 13; -fx-padding: 10 14 10 14;"
                     + "-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label status = new Label("");
        status.setWrapText(true);
        status.setVisible(false); status.setManaged(false);

        Button send = new Button("Send Recovery Code");
        send.setMaxWidth(Double.MAX_VALUE);
        send.setStyle("-fx-background-color: #000080; -fx-text-fill: white; -fx-font-weight: bold;"
                    + "-fx-padding: 10 0 10 0; -fx-background-radius: 8; -fx-cursor: hand;");

        Button cancel = new Button("Cancel");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-font-weight: bold;"
                      + "-fx-padding: 10 0 10 0; -fx-background-radius: 8; -fx-cursor: hand;");
        cancel.setOnAction(e -> dialog.close());

        send.setOnAction(e -> {
            String target = email.getText().trim();
            if (target.isEmpty()) {
                status.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");
                status.setText("Please enter your email.");
                status.setVisible(true); status.setManaged(true);
                return;
            }
            PasswordResetService.Result r = PasswordResetService.startRecovery(target);
            status.setVisible(true); status.setManaged(true);
            switch (r) {
                case SENT -> {
                    status.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12;");
                    status.setText("A recovery code has been sent to " + target + ".");
                }
                case SMTP_DISABLED -> {
                    status.setStyle("-fx-text-fill: #eab308; -fx-font-size: 12;");
                    status.setText("Recovery code generated but SMTP is disabled. "
                        + "Super Admin can enable email in SMTP Settings. Code was logged to console.");
                }
                case EMAIL_NOT_FOUND -> {
                    status.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12;");
                    // Show same success message to avoid leaking account existence.
                    status.setText("If that email exists, a recovery code has been sent.");
                }
                default -> {
                    status.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");
                    status.setText("Could not process recovery. Please try again later.");
                }
            }
        });

        VBox box = new VBox(12, title, hint, email, status, send, cancel);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-background-color: white;");
        box.setPrefWidth(420);

        dialog.setScene(new Scene(box));
        dialog.showAndWait();
    }
}
