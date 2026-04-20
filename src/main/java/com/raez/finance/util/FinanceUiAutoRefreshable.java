package com.raez.finance.util;

/**
 * Views that should reload server-backed data on a timer (see FinanceMainLayoutController).
 */
@FunctionalInterface
public interface FinanceUiAutoRefreshable {

    void refreshVisibleData();
}
