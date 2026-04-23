package com.raez.finance.dao;

import com.raez.finance.model.FinanceProductReportRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface FinanceProductDaoInterface {
    List<FinanceProductReportRow> findReportRows(LocalDate from, LocalDate to, String categoryFilter, String search) throws SQLException;
    List<FinanceProductReportRow> findReportRows(LocalDate from, LocalDate to, String categoryFilter, String search, int limit, int offset) throws SQLException;
    int countReportRows(LocalDate from, LocalDate to, String categoryFilter, String search) throws SQLException;
    List<FinanceProductDao.CategoryRevenueProfit> findCategoryRevenueProfit() throws SQLException;
    List<FinanceProductDao.CategoryRevenueProfit> findCategoryRevenueProfit(LocalDate from, LocalDate to) throws SQLException;
    List<String> findCategoryNames() throws SQLException;
}
