package com.raez.reviews.service;

import com.raez.reviews.dao.SettingsDao;
import com.raez.reviews.exception.BusinessException;

public class SettingsService {
    private final SettingsDao settingsDao;

    public SettingsService(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    public int getReviewEditWindowMinutes() {
        return settingsDao.getReviewEditWindowMinutes();
    }

    public void updateReviewEditWindowMinutes(int minutes) {
        if (minutes < 1 || minutes > 120) {
            throw new BusinessException("The review edit window must be between 1 and 120 minutes.");
        }
        settingsDao.updateReviewEditWindowMinutes(minutes);
    }
}
