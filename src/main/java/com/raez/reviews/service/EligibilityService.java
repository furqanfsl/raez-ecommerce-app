package com.raez.reviews.service;

import java.util.Optional;

import com.raez.reviews.dao.OrderDao;
import com.raez.reviews.dao.ReviewDao;
import com.raez.reviews.exception.BusinessException;
import com.raez.reviews.model.Review;

public class EligibilityService {
    private final OrderDao orderDao;
    private final ReviewDao reviewDao;

    public EligibilityService(OrderDao orderDao, ReviewDao reviewDao) {
        this.orderDao = orderDao;
        this.reviewDao = reviewDao;
    }

    public boolean hasPurchasedProduct(int customerId, int productId) {
        return orderDao.hasPurchasedProduct(customerId, productId);
    }

    public Optional<Review> getExistingReview(int customerId, int productId) {
        return reviewDao.findByCustomerAndProduct(customerId, productId);
    }

    public void ensureCustomerCanAddReview(int customerId, int productId) {
        if (!hasPurchasedProduct(customerId, productId)) {
            throw new BusinessException("You can rate this product after purchasing it.");
        }
        if (reviewDao.existsByCustomerAndProduct(customerId, productId)) {
            throw new BusinessException("You have already reviewed this product.");
        }
    }
}
