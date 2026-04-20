package com.raez.finance.dao;

import java.util.List;

public interface FinanceInventorySupplierDaoInterface {
    List<FinanceInventorySupplierDao.SupplierSnapshot> findSuppliers() throws Exception;
    double getCurrentStockValue() throws Exception;
    List<FinanceInventorySupplierDao.LowStockSnapshot> findLowStockItems() throws Exception;

    int countActiveProducts() throws Exception;
    int countSuppliers() throws Exception;
    int countLowStockProducts() throws Exception;
    int countOutOfStockProducts() throws Exception;
    List<FinanceInventorySupplierDao.ProductInventoryRow> findProductInventoryRows() throws Exception;
}
