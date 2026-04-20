package com.raez.finance.dao;

import com.raez.finance.model.FinanceOrderReportRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface FinanceOrderDaoInterface {
    List<FinanceOrderReportRow> findReportRows(LocalDate from, LocalDate to, String statusFilter, String categoryFilter, String search, int limit, int offset) throws SQLException;
    int countReportRows(LocalDate from, LocalDate to, String statusFilter, String categoryFilter, String search) throws SQLException;
    List<String> findStatusOptions() throws SQLException;
}
