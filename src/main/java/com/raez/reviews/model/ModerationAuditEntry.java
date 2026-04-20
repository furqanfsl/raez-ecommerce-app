package com.raez.reviews.model;

import java.time.LocalDateTime;

public class ModerationAuditEntry {
    private final int auditId;
    private final int reviewId;
    private final int adminUserId;
    private final String adminName;
    private final String action;
    private final String reason;
    private final LocalDateTime actionTime;

    public ModerationAuditEntry(int auditId, int reviewId, int adminUserId, String adminName, String action, String reason,
            LocalDateTime actionTime) {
        this.auditId = auditId;
        this.reviewId = reviewId;
        this.adminUserId = adminUserId;
        this.adminName = adminName;
        this.action = action;
        this.reason = reason;
        this.actionTime = actionTime;
    }

    public int getAuditId() {
        return auditId;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getAdminUserId() {
        return adminUserId;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }
}
