package com.raez.finance.dao;

import com.raez.finance.model.FinanceCustomerReportRow;
import com.raez.finance.model.FinanceTopBuyerRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface FinanceCustomerDaoInterface {
    List<FinanceCustomerReportRow> findReportRows(LocalDate orderFrom, LocalDate orderTo, String typeFilter, String countryFilter, String companyName, String search, int limit, int offset) throws SQLException;
    List<String> findCompanyNames() throws SQLException;
    int countReportRows(LocalDate orderFrom, LocalDate orderTo, String typeFilter, String countryFilter, String companyName, String search) throws SQLException;
    List<FinanceTopBuyerRow> findTopBuyers(int limit) throws SQLException;
    List<FinanceTopBuyerRow> findTopBuyersInRange(LocalDate from, LocalDate to, String insightCustomerTypeFilter, int limit, int offset) throws SQLException;
    int countTopBuyersInRange(LocalDate from, LocalDate to, String insightCustomerTypeFilter) throws SQLException;
    double sumOrderTotalInBuyerFilterRange(LocalDate from, LocalDate to, String insightCustomerTypeFilter) throws SQLException;
    List<FinanceCustomerDao.MonthlyCount> findMonthlyOrderCounts() throws SQLException;
    List<FinanceCustomerDao.MonthlyCount> findMonthlyOrderCounts(LocalDate from, LocalDate to) throws SQLException;
    List<FinanceCustomerDao.MonthlySplit> findMonthlyOrderCountsByCustomerType(LocalDate from, LocalDate to) throws SQLException;
    int getTotalCustomerCount() throws SQLException;
    int getCompanyCustomerCount() throws SQLException;
    double getTotalRevenue() throws SQLException;
    List<String> findRefundAlerts() throws SQLException;
    List<String> findProductIssueAlerts() throws SQLException;
    List<String> findCountryOptions() throws SQLException;
}
