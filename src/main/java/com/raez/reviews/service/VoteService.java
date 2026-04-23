package com.raez.reviews.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.raez.reviews.dao.ReviewDao;
import com.raez.reviews.dao.VoteDao;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Review;
import com.raez.reviews.model.VoteType;
import com.raez.reviews.util.DatabaseManager;

public class VoteService {
    private final DatabaseManager databaseManager;
    private final ReviewDao reviewDao;
    private final VoteDao voteDao;

    public VoteService(DatabaseManager databaseManager, ReviewDao reviewDao, VoteDao voteDao) {
        this.databaseManager = databaseManager;
        this.reviewDao = reviewDao;
        this.voteDao = voteDao;
    }

    public void addVote(int customerId, int reviewId, VoteType voteType) {
        Review review = reviewDao.findById(reviewId)
                .orElseThrow(() -> new BusinessException("The selected review could not be found."));
        if (!review.getStatus().visibleToCustomers()) {
            throw new BusinessException("You cannot vote on a removed review.");
        }
        if (review.getCustomerId() == customerId) {
            throw new BusinessException("You cannot vote on your own review.");
        }
        if (voteDao.hasExistingVote(reviewId, customerId)) {
            throw new BusinessException("You have already voted on this review.");
        }

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            voteDao.insert(connection, reviewId, customerId, voteType);
            reviewDao.incrementVoteCount(connection, reviewId, voteType == VoteType.HELPFUL);
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save the selected vote.", exception);
        }
    }
}
