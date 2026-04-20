package com.raez.finance.dao;

import java.util.List;

public interface FinanceFinancialAnomalyDaoInterface {
    List<FinanceFinancialAnomalyDao.AnomalyRow> findAnomalies(boolean unresolvedOnly) throws Exception;
    void setResolved(int anomalyId, boolean resolved) throws Exception;
    int countUnresolved() throws Exception;
}
