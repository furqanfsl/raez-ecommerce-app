package com.raez.finance.dao;

import java.time.LocalDate;
import java.util.List;

public interface FinanceRevenueVatDaoInterface {
    List<FinanceRevenueVatDao.CategoryVatRow> findCategoryVatRows(LocalDate from, LocalDate to) throws Exception;
}
