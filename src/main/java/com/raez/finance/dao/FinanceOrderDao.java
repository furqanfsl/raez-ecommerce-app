package com.raez.finance.dao;

import com.raez.finance.model.FinanceOrderReportRow;
import com.raez.finance.util.FinanceDatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches order report rows for Detailed Reports (orders tab).
 */
public class FinanceOrderDao implements FinanceOrderDaoInterface {

    private static final String CATEGORY_CLAUSE =
        "AND (? IS NULL OR EXISTS ( " +
        "SELECT 1 FROM order_items oi " +
        "JOIN products p ON oi.productID = p.productID " +
        "LEFT JOIN categories cat ON p.categoryID = cat.categoryID " +
        "WHERE oi.orderID = o.orderID " +
        "AND COALESCE(cat.categoryName, 'Uncategorized') = ? " +
        ")) ";

    private static final String FIND_SQL =
        "SELECT o.orderID, c.name AS customerName, o.totalAmount, o.orderDate, o.status " +
        "FROM \"orders\" o " +
        "JOIN customers c ON o.customerID = c.customerID " +
        "WHERE (? IS NULL OR o.orderDate >= ?) " +
        "AND (? IS NULL OR o.orderDate <= ?) " +
        "AND (? IS NULL OR o.status = ?) " +
        "AND (? IS NULL OR (c.name LIKE ? OR CAST(o.orderID AS TEXT) LIKE ?)) " +
        CATEGORY_CLAUSE +
        "ORDER BY o.orderDate DESC, o.orderID DESC " +
        "LIMIT ? OFFSET ?";

    private static final String FIND_NO_LIMIT_SQL =
        "SELECT o.orderID, c.name AS customerName, o.totalAmount, o.orderDate, o.status " +
        "FROM \"orders\" o " +
        "JOIN customers c ON o.customerID = c.customerID " +
        "WHERE (? IS NULL OR o.orderDate >= ?) " +
        "AND (? IS NULL OR o.orderDate <= ?) " +
        "AND (? IS NULL OR o.status = ?) " +
        "AND (? IS NULL OR (c.name LIKE ? OR CAST(o.orderID AS TEXT) LIKE ?)) " +
        CATEGORY_CLAUSE +
        "ORDER BY o.orderDate DESC, o.orderID DESC";

    private static final String COUNT_SQL =
        "SELECT COUNT(*) " +
        "FROM \"orders\" o " +
        "JOIN customers c ON o.customerID = c.customerID " +
        "WHERE (? IS NULL OR o.orderDate >= ?) " +
        "AND (? IS NULL OR o.orderDate <= ?) " +
        "AND (? IS NULL OR o.status = ?) " +
        "AND (? IS NULL OR (c.name LIKE ? OR CAST(o.orderID AS TEXT) LIKE ?)) " +
        CATEGORY_CLAUSE;

    /**
     * Fetch orders with optional date range, status filter, category (product line), and search.
     * Use limit &lt;= 0 for no limit.
     */
    public List<FinanceOrderReportRow> findReportRows(LocalDate from, LocalDate to, String statusFilter, String categoryFilter, String search, int limit, int offset) throws SQLException {
        String fromParam = from == null ? null : from + " 00:00:00";
        String toParam = to == null ? null : to + " 23:59:59";
        String statusParam = normalizeStatus(statusFilter);
        String searchLike = normalizeSearchLike(search);
        String[] cat = normalizeCategoryPair(categoryFilter);
        String sql = limit > 0 ? FIND_SQL : FIND_NO_LIMIT_SQL;

        List<FinanceOrderReportRow> rows = new ArrayList<>();
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, fromParam);
            ps.setString(i++, fromParam);
            ps.setString(i++, toParam);
            ps.setString(i++, toParam);
            ps.setString(i++, statusParam);
            ps.setString(i++, statusParam);
            ps.setString(i++, searchLike);
            ps.setString(i++, searchLike);
            ps.setString(i++, searchLike);
            ps.setString(i++, cat[0]);
            ps.setString(i++, cat[1]);
            if (limit > 0) {
                ps.setInt(i++, limit);
                ps.setInt(i, offset);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int orderId = rs.getInt("orderID");
                String productSummary = getProductSummary(conn, orderId);
                rows.add(new FinanceOrderReportRow(
                        String.valueOf(orderId),
                        rs.getString("customerName"),
                        productSummary,
                        rs.getDouble("totalAmount"),
                        formatDate(rs.getString("orderDate")),
                        rs.getString("status")
                ));
            }
        }
        return rows;
    }

    /** Count orders matching the same filters as findReportRows (for pagination). */
    public int countReportRows(LocalDate from, LocalDate to, String statusFilter, String categoryFilter, String search) throws SQLException {
        String fromParam = from == null ? null : from + " 00:00:00";
        String toParam = to == null ? null : to + " 23:59:59";
        String statusParam = normalizeStatus(statusFilter);
        String searchLike = normalizeSearchLike(search);
        String[] cat = normalizeCategoryPair(categoryFilter);
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_SQL)) {
            int i = 1;
            ps.setString(i++, fromParam);
            ps.setString(i++, fromParam);
            ps.setString(i++, toParam);
            ps.setString(i++, toParam);
            ps.setString(i++, statusParam);
            ps.setString(i++, statusParam);
            ps.setString(i++, searchLike);
            ps.setString(i++, searchLike);
            ps.setString(i++, searchLike);
            ps.setString(i++, cat[0]);
            ps.setString(i++, cat[1]);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** When "All Categories", both null so (? IS NULL OR EXISTS(...)) is true. */
    private String[] normalizeCategoryPair(String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isBlank() || "All Categories".equalsIgnoreCase(categoryFilter.trim())) {
            return new String[]{null, null};
        }
        String name = categoryFilter.trim();
        return new String[]{name, name};
    }

    private String normalizeStatus(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) return null;
        if ("All Status".equalsIgnoreCase(statusFilter.trim())) return null;
        return statusFilter.trim();
    }

    private String normalizeSearchLike(String search) {
        if (search == null || search.isBlank()) return null;
        return "%" + search.trim() + "%";
    }

    private String getProductSummary(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT p.name, (SELECT COUNT(*) FROM order_items WHERE orderID = ?) AS cnt " +
                "FROM order_items oi JOIN products p ON oi.productID = p.productID " +
                "WHERE oi.orderID = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String first = rs.getString("name");
                int cnt = rs.getInt("cnt");
                return cnt > 1 ? first + " +" + (cnt - 1) + " more" : first;
            }
        }
        return "—";
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null) return "—";
        if (dateStr.length() >= 10) return dateStr.substring(0, 10);
        return dateStr;
    }

    /** Distinct order statuses for the Status ComboBox (UI adds "All Status"). */
    public List<String> findStatusOptions() throws SQLException {
        String sql = "SELECT DISTINCT status FROM \"orders\" WHERE status IS NOT NULL ORDER BY status";
        List<String> list = new ArrayList<>();
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("status"));
            }
        }
        return list;
    }
}
