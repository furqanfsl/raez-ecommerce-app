package com.raez.reviews.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.raez.reviews.dao.ProductDao;
import com.raez.reviews.dao.ReviewDao;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Product;
import com.raez.reviews.model.ProductReviewSummary;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewSortOption;
import com.raez.reviews.model.ReviewStatus;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.TimeUtils;
import com.raez.reviews.util.ValidationUtils;

public class ReviewService {
    private final DatabaseManager databaseManager;
    private final ProductDao productDao;
    private final ReviewDao reviewDao;
    private final EligibilityService eligibilityService;
    private final SettingsService settingsService;

    public ReviewService(DatabaseManager databaseManager, ProductDao productDao, ReviewDao reviewDao,
            EligibilityService eligibilityService, SettingsService settingsService) {
        this.databaseManager = databaseManager;
        this.productDao = productDao;
        this.reviewDao = reviewDao;
        this.eligibilityService = eligibilityService;
        this.settingsService = settingsService;
    }

    public List<Product> getProducts(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return productDao.findAllActive();
        }
        return productDao.findActiveBySearch(searchText.trim());
    }

    public List<Review> getReviewsForProduct(int productId, ReviewSortOption sortOption) {
        return reviewDao.findForProduct(productId, sortOption);
    }

    public List<Review> getReviewsByCustomer(int customerId) {
        return reviewDao.findByCustomer(customerId);
    }

    public Optional<Review> getCustomerReviewForProduct(int customerId, int productId) {
        return reviewDao.findByCustomerAndProduct(customerId, productId);
    }

    public ProductReviewSummary getProductSummary(int productId) {
        Product product = productDao.findById(productId)
                .orElseThrow(() -> new BusinessException("The selected product could not be found."));
        return new ProductReviewSummary(product, reviewDao.calculateDistribution(productId));
    }

    public boolean hasPurchasedProduct(int customerId, int productId) {
        return eligibilityService.hasPurchasedProduct(customerId, productId);
    }

    public int getEditWindowMinutes() {
        return settingsService.getReviewEditWindowMinutes();
    }

    public int createReview(int customerId, int productId, int rating, String comment) {
        ValidationUtils.validateRating(rating);
        ValidationUtils.validateComment(comment);
        eligibilityService.ensureCustomerCanAddReview(customerId, productId);

        LocalDateTime now = LocalDateTime.now().withNano(0);
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            int reviewId = reviewDao.insert(connection, productId, customerId, rating, comment.trim(), TimeUtils.toStorage(now));
            connection.commit();
            return reviewId;
        } catch (SQLException exception) {
            throw translateDatabaseFailure("Unable to create the review.", exception);
        }
    }

    public void editReview(int customerId, int reviewId, int rating, String comment) {
        ValidationUtils.validateRating(rating);
        ValidationUtils.validateComment(comment);

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            Review review = reviewDao.findById(connection, reviewId)
                    .orElseThrow(() -> new BusinessException("The selected review could not be found."));
            ensureCustomerCanManageReview(customerId, review);
            reviewDao.updateCustomerReview(connection, reviewId, rating, comment.trim(), TimeUtils.toStorage(LocalDateTime.now().withNano(0)));
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update the review.", exception);
        }
    }

    public void deleteReview(int customerId, int reviewId) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            Review review = reviewDao.findById(connection, reviewId)
                    .orElseThrow(() -> new BusinessException("The selected review could not be found."));
            ensureCustomerCanManageReview(customerId, review);
            reviewDao.updateStatus(connection, reviewId, ReviewStatus.CUSTOMER_DELETED, TimeUtils.toStorage(LocalDateTime.now().withNano(0)));
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete the review.", exception);
        }
    }

    public boolean isEditableByCustomer(Review review, int customerId) {
        return review != null
                && review.getCustomerId() == customerId
                && review.getStatus() != ReviewStatus.REMOVED
                && review.getStatus() != ReviewStatus.CUSTOMER_DELETED
                && !getRemainingEditDuration(review, customerId).isNegative()
                && !getRemainingEditDuration(review, customerId).isZero();
    }

    public Duration getRemainingEditDuration(Review review, int customerId) {
        if (review == null || review.getCustomerId() != customerId) {
            return Duration.ZERO;
        }
        // The editable window is measured from the original submission time, not the last edit time.
        LocalDateTime deadline = review.getCreatedAt().plusMinutes(settingsService.getReviewEditWindowMinutes());
        Duration remaining = Duration.between(LocalDateTime.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void ensureCustomerCanManageReview(int customerId, Review review) {
        if (review.getCustomerId() != customerId) {
            throw new BusinessException("You can only manage your own reviews.");
        }
        if (!isEditableByCustomer(review, customerId)) {
            throw new BusinessException("The review edit window has expired.");
        }
    }

    private RuntimeException translateDatabaseFailure(String defaultMessage, SQLException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("UNIQUE")) {
            return new BusinessException("You have already reviewed this product.");
        }
        return new IllegalStateException(defaultMessage, exception);
    }
}
