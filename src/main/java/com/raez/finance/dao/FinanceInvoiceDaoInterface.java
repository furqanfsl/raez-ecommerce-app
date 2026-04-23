package com.raez.finance.dao;

import java.time.LocalDate;
import java.util.List;

public interface FinanceInvoiceDaoInterface {
    List<FinanceInvoiceDao.InvoiceRow> findInvoices(LocalDate from, LocalDate to, String statusFilter, String search, int limit, int offset) throws Exception;

    FinanceInvoiceDao.InvoiceKpiRow aggregateForRange(LocalDate from, LocalDate to, String statusFilter, String search) throws Exception;

    List<FinanceInvoiceDao.OrderWithoutInvoiceRow> findOrdersWithoutInvoice(int limit) throws Exception;

    int insertInvoiceForOrder(int orderId, LocalDate dueDate, String notes) throws Exception;

    void updateInvoice(int invoiceId, String status, LocalDate dueDate, String notes) throws Exception;

    void markInvoicePaid(int invoiceId) throws Exception;
}
