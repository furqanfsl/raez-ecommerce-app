package Ecommerce1.Ecommerce1.service;

import Ecommerce1.Ecommerce1.model.Warehouse_LowStockRow;
import Ecommerce1.Ecommerce1.model.Warehouse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javafx.collections.ObservableList;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ReportService — generates a PDF inventory report.
 *
 * OOP principles applied:
 *  - Single Responsibility: only handles PDF generation
 *  - Encapsulation: all PDF logic is private, only generateReport() is public
 */
public class Warehouse_ReportService {

    // ── Colours ──
    private static final BaseColor HEADER_BG    = new BaseColor(37, 99, 235);   // blue
    private static final BaseColor ALERT_BG     = new BaseColor(254, 226, 226); // red-100
    private static final BaseColor ALERT_TEXT   = new BaseColor(153, 27, 27);   // red-800
    private static final BaseColor ROW_ALT      = new BaseColor(249, 250, 251); // gray-50
    private static final BaseColor BORDER_COLOR = new BaseColor(229, 231, 235); // gray-200
    private static final BaseColor GREEN        = new BaseColor(22, 163, 74);
    private static final BaseColor RED          = new BaseColor(220, 38, 38);

    // ── Fonts ──
    private static final Font TITLE_FONT     = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.BLACK);
    private static final Font SUBTITLE_FONT  = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(107, 114, 128));
    private static final Font SECTION_FONT   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.BLACK);
    private static final Font HEADER_FONT    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font CELL_FONT      = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.BLACK);
    private static final Font LABEL_FONT     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(75, 85, 99));
    private static final Font VALUE_FONT     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
    private static final Font ALERT_FONT     = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, ALERT_TEXT);
    private static final Font GREEN_FONT     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   GREEN);
    private static final Font RED_FONT       = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   RED);

    /**
     * Generates the PDF report and saves it to the given file path.
     *
     * @param filePath      where to save the PDF
     * @param warehouses    current warehouse list
     * @param warehouse_LowStockRows  current low stock rows
     * @param totalStock    total stock value
     * @param lowStockCount number of low stock items
     * @param capacityPct   capacity usage percentage
     */
    public void generateReport(String filePath,
                                ObservableList<Warehouse> warehouses,
                                List<Warehouse_LowStockRow> warehouse_LowStockRows,
                                int totalStock,
                                int lowStockCount,
                                int capacityPct) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        // ── Title ──
        addTitle(doc);

        // ── Summary section ──
        addSummary(doc, totalStock, lowStockCount, capacityPct, warehouses.size());

        // ── Capacity per warehouse ──
        addWarehouseCapacity(doc, warehouses);

        // ── Low stock alert ──
        if (!warehouse_LowStockRows.isEmpty()) {
            addLowStockSection(doc, warehouse_LowStockRows);
        }

        // ── Full product list per warehouse ──
        addProductsByWarehouse(doc, warehouses);

        doc.close();
    }

    // ====================================================
    // PRIVATE SECTIONS
    // ====================================================

    private void addTitle(Document doc) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("Inventory & Warehousing Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(4);
        doc.add(title);

        // Subtitle with date
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"));
        Paragraph subtitle = new Paragraph("Generated on " + date, SUBTITLE_FONT);
        subtitle.setSpacingAfter(16);
        doc.add(subtitle);

        // Divider line
        doc.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(
                0.5f, 100, BORDER_COLOR, Element.ALIGN_CENTER, -2)));
        doc.add(Chunk.NEWLINE);
    }

    private void addSummary(Document doc, int totalStock, int lowStockCount,
                             int capacityPct, int warehouseCount) throws DocumentException {
        doc.add(sectionLabel("Summary"));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(16);

        addSummaryCell(table, "Total Stock",       totalStock + " units",    BLACK_FONT());
        addSummaryCell(table, "Warehouses",        warehouseCount + "",      BLACK_FONT());
        addSummaryCell(table, "Low Stock Items",   lowStockCount + "",       lowStockCount > 0 ? RED_FONT : GREEN_FONT);
        addSummaryCell(table, "Capacity Usage",    capacityPct + "%",        capacityPct > 80 ? RED_FONT : GREEN_FONT);

        doc.add(table);
    }

    private void addWarehouseCapacity(Document doc,
                                       ObservableList<Warehouse> warehouses) throws DocumentException {
        doc.add(sectionLabel("Warehouse Capacity"));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2.5f, 1.5f, 1.5f, 1.5f});
        table.setSpacingAfter(16);

        // Header
        addHeaderCell(table, "Warehouse ID");
        addHeaderCell(table, "Warehouse Name");
        addHeaderCell(table, "Current Stock");
        addHeaderCell(table, "Max Capacity");
        addHeaderCell(table, "Usage %");

        // Rows
        boolean alt = false;
        for (Warehouse w : warehouses) {
            BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
            addCell(table, w.getWarehouseId(),                bg);
            addCell(table, w.getName(),                       bg);
            addCell(table, String.valueOf(w.getCurrentStock()), bg);
            addCell(table, String.valueOf(w.getMaxCapacity()), bg);

            int pct = w.getCapacityPercentage();
            PdfPCell pctCell = styledCell(pct + "%", pct > 80 ? RED_FONT : GREEN_FONT, bg);
            table.addCell(pctCell);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addLowStockSection(Document doc,
                                     List<Warehouse_LowStockRow> warehouse_LowStockRows) throws DocumentException {
        // Alert header
        Paragraph alert = new Paragraph("⚠  Urgent Stock Items – Immediate Attention Required", ALERT_FONT);
        alert.setSpacingBefore(4);
        alert.setSpacingAfter(6);

        PdfPCell alertCell = new PdfPCell(alert);
        alertCell.setBackgroundColor(ALERT_BG);
        alertCell.setBorderColor(new BaseColor(252, 165, 165));
        alertCell.setPadding(8);

        PdfPTable alertTable = new PdfPTable(1);
        alertTable.setWidthPercentage(100);
        alertTable.setSpacingAfter(6);
        alertTable.addCell(alertCell);
        doc.add(alertTable);

        // Low stock table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 2f});
        table.setSpacingAfter(16);

        addHeaderCell(table, "Product Name");
        addHeaderCell(table, "SKU");
        addHeaderCell(table, "Current Stock");
        addHeaderCell(table, "Warehouse Location");

        boolean alt = false;
        for (Warehouse_LowStockRow row : warehouse_LowStockRows) {
            BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
            addCell(table, row.getProductName(),                     bg);
            addCell(table, row.getSku(),                             bg);
            PdfPCell qtyCell = styledCell(String.valueOf(row.getQty()), RED_FONT, bg);
            table.addCell(qtyCell);
            addCell(table, row.getWarehouseLocation(),               bg);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addProductsByWarehouse(Document doc,
                                         ObservableList<Warehouse> warehouses) throws DocumentException {
        doc.add(sectionLabel("Products by Warehouse"));

        for (Warehouse w : warehouses) {
            if (w.getProducts().isEmpty()) continue;

            // Warehouse sub-header
            Paragraph whHeader = new Paragraph(w.getName() + " — " + w.getLocation(), VALUE_FONT);
            whHeader.setSpacingBefore(8);
            whHeader.setSpacingAfter(4);
            doc.add(whHeader);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 1.5f, 1.5f});
            table.setSpacingAfter(12);

            addHeaderCell(table, "Product Name");
            addHeaderCell(table, "SKU");
            addHeaderCell(table, "Qty On Hand");
            addHeaderCell(table, "Min Threshold");
            addHeaderCell(table, "Status");

            boolean alt = false;
            for (Ecommerce1.Ecommerce1.model.Warehouse_Product p : w.getProducts()) {
                BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
                addCell(table, p.getName(),                      bg);
                addCell(table, p.getSku(),                       bg);
                addCell(table, String.valueOf(p.getQuantity()),  bg);
                addCell(table, String.valueOf(p.getMinThreshold()), bg);

                boolean low = p.isLowStock();
                PdfPCell statusCell = styledCell(low ? "Low Stock" : "In Stock",
                        low ? RED_FONT : GREEN_FONT, bg);
                table.addCell(statusCell);
                alt = !alt;
            }

            doc.add(table);
        }
    }

    // ====================================================
    // HELPERS
    // ====================================================

    private Paragraph sectionLabel(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(8);
        p.setSpacingAfter(8);
        return p;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(7);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, CELL_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private PdfPCell styledCell(String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(10);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", LABEL_FONT));
        p.add(new Chunk(value, valueFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private Font BLACK_FONT() {
        return new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
    }
}