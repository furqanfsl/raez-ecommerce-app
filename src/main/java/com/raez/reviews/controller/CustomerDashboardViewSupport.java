package com.raez.reviews.controller;

import java.time.Duration;

import com.raez.reviews.model.ProductReviewSummary;
import com.raez.reviews.model.Review;
import com.raez.reviews.util.TimeUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

final class CustomerDashboardViewSupport {
    private CustomerDashboardViewSupport() {
    }

    static void configureReviewTable(TableView<Review> reviewTable, TableColumn<Review, String> reviewerColumn,
            TableColumn<Review, Number> ratingColumn, TableColumn<Review, String> commentColumn,
            TableColumn<Review, Number> helpfulColumn, TableColumn<Review, Number> unhelpfulColumn,
            TableColumn<Review, String> statusColumn, TableColumn<Review, String> createdColumn) {
        reviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        reviewerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        ratingColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRating()));
        commentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComment()));
        helpfulColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getHelpfulCount()));
        unhelpfulColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getUnhelpfulCount()));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        createdColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(TimeUtils.toDisplay(cellData.getValue().getCreatedAt())));
    }

    static void clearProductDetails(TableView<Review> reviewTable, Label averageRatingLabel, Label reviewCountLabel,
            Label eligibilityLabel, Label editWindowLabel, BarChart<String, Number> ratingChart) {
        reviewTable.getItems().clear();
        averageRatingLabel.setText("Average rating: -");
        reviewCountLabel.setText("Visible reviews: 0");
        eligibilityLabel.setText("Search for a product to inspect reviews.");
        editWindowLabel.setText("Select your own review to view the edit timer.");
        ratingChart.getData().clear();
    }

    static void updateSummaryLabels(ProductReviewSummary summary, Label averageRatingLabel, Label reviewCountLabel) {
        averageRatingLabel.setText(String.format("Average rating: %.2f / 5", summary.getProduct().getAverageRating()));
        reviewCountLabel.setText("Visible reviews: " + summary.getProduct().getReviewCount());
    }

    static void updateRatingChart(BarChart<String, Number> ratingChart, ProductReviewSummary summary) {
        ratingChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int[] buckets = summary.getRatingBuckets();
        for (int index = 0; index < buckets.length; index++) {
            series.getData().add(new XYChart.Data<>(Integer.toString(index + 1) + " star", buckets[index]));
        }
        ratingChart.getData().add(series);
    }

    static String formatEditWindow(Duration remaining) {
        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).getSeconds();
        return String.format("Edit window remaining: %02d:%02d", minutes, seconds);
    }
}
