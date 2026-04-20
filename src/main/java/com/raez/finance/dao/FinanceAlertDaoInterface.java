package com.raez.finance.dao;

import java.util.List;

public interface FinanceAlertDaoInterface {
    List<FinanceAlertDao.AlertRow> findAlerts(boolean unresolvedOnly) throws Exception;
    void setResolved(int alertId, boolean resolved) throws Exception;
    int countUnresolved() throws Exception;
}
