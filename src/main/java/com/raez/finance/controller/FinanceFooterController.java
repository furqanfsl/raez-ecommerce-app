package com.raez.finance.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinanceFooterController {
    private static final Logger log = LoggerFactory.getLogger(FinanceFooterController.class);


    @FXML
    public void initialize() {
        // FinanceFooter doesn't need much initialization, but the method is here for JavaFX standards.
    }

    @FXML
    private void handlePrivacyPolicy(ActionEvent event) {
        log.info("{}", "Opening Privacy Policy...");
        // Cursor can add logic to open a web browser or show a modal
    }

    @FXML
    private void handleTermsOfService(ActionEvent event) {
        log.info("{}", "Opening Terms of Service...");
    }

    @FXML
    private void handleSupport(ActionEvent event) {
        log.info("{}", "Opening Support Portal...");
    }
}