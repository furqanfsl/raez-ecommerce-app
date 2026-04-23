package com.raez.reviews.controller;

import java.util.function.Consumer;

import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewDraft;
import com.raez.reviews.util.ValidationUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ReviewFormController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label helperLabel;
    @FXML
    private Spinner<Integer> ratingSpinner;
    @FXML
    private TextArea commentArea;
    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private Consumer<ReviewDraft> onSave;

    @FXML
    private void initialize() {
        ratingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));
        commentArea.setWrapText(true);
    }

    public void init(Stage dialogStage, String title, Review existingReview, boolean adminMode, Consumer<ReviewDraft> onSave) {
        this.dialogStage = dialogStage;
        this.onSave = onSave;
        titleLabel.setText(title);
        helperLabel.setText(adminMode
                ? "Admin edits can correct rating and review text before restoring visibility."
                : "Reviews must be between 1 and 500 characters and cannot include blocked special characters.");
        if (existingReview != null) {
            ratingSpinner.getValueFactory().setValue(existingReview.getRating());
            commentArea.setText(existingReview.getComment());
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            int rating = ratingSpinner.getValue();
            String comment = commentArea.getText() == null ? "" : commentArea.getText().trim();
            ValidationUtils.validateRating(rating);
            ValidationUtils.validateComment(comment);
            onSave.accept(new ReviewDraft(rating, comment));
            dialogStage.close();
        } catch (BusinessException exception) {
            errorLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}
