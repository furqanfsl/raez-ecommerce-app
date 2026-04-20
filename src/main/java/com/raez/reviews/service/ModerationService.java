package com.raez.reviews.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.raez.reviews.dao.ModerationAuditDao;
import com.raez.reviews.dao.ReviewDao;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.ModerationAuditEntry;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.ReviewStatus;
import com.raez.reviews.util.DatabaseManager;
import com.raez.reviews.util.TimeUtils;
import com.raez.reviews.util.ValidationUtils;

public class ModerationService {
    private final DatabaseManager databaseManager;
    private final ReviewDao reviewDao;
    private final ModerationAuditDao moderationAuditDao;

    public ModerationService(DatabaseManager databaseManager, ReviewDao reviewDao, ModerationAuditDao moderationAuditDao) {
        this.databaseManager = databaseManager;
        this.reviewDao = reviewDao;
        this.moderationAuditDao = moderationAuditDao;
    }

    public List<Review> getReviews(Integer productId, ReviewStatus status, String searchText) {
        return reviewDao.findForAdmin(productId, status, searchText);
    }

    public List<ModerationAuditEntry> getAuditEntries() {
        return moderationAuditDao.findAll();
    }

    public void flagReview(int adminId, int reviewId, String reason) {
        applyStatusChange(adminId, reviewId, ReviewStatus.FLAGGED, "FLAGGED", reason);
    }

    public void removeReview(int adminId, int reviewId, String reason) {
        applyStatusChange(adminId, reviewId, ReviewStatus.REMOVED, "REMOVED", reason);
    }

    public void restoreReview(int adminId, int reviewId, String reason) {
        applyStatusChange(adminId, reviewId, ReviewStatus.ACTIVE, "RESTORED", reason);
    }

    public void editReview(int adminId, int reviewId, int rating, String comment, String reason) {
        ValidationUtils.validateRating(rating);
        ValidationUtils.validateComment(comment);
        String auditTime = TimeUtils.toStorage(LocalDateTime.now().withNano(0));

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            Review review = reviewDao.findById(connection, reviewId)
                    .orElseThrow(() -> new BusinessException("The selected review could not be found."));
            reviewDao.updateAdminReview(connection, reviewId, rating, comment.trim(), ReviewStatus.ACTIVE, auditTime);
            moderationAuditDao.insert(connection, reviewId, adminId, "EDITED", reason == null || reason.isBlank() ? "Admin correction" : reason, auditTime);
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to edit the selected review.", exception);
        }
    }

    private void applyStatusChange(int adminId, int reviewId, ReviewStatus status, String action, String reason) {
        String finalReason = reason == null || reason.isBlank() ? action + " by admin" : reason;
        String auditTime = TimeUtils.toStorage(LocalDateTime.now().withNano(0));

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            // Moderation changes update both the visible review state and the audit trail before commit.
            Review review = reviewDao.findById(connection, reviewId)
                    .orElseThrow(() -> new BusinessException("The selected review could not be found."));
            reviewDao.updateStatus(connection, reviewId, status, auditTime);
            moderationAuditDao.insert(connection, reviewId, adminId, action, finalReason, auditTime);
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update the moderation status.", exception);
        }
    }
}
