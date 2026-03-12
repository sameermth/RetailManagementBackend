package com.retailmanagement.common.utils;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PdfGenerator {

    public byte[] generateReport(Map<String, Object> data, String templateName) {
        log.debug("Generating PDF report for template: {}", templateName);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Add title
            String title = data.get("reportTitle") != null ?
                    data.get("reportTitle").toString() : "Report";
            document.add(new Paragraph(title)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold());

            // Add date range if available
            if (data.containsKey("startDate") && data.containsKey("endDate")) {
                document.add(new Paragraph("Period: " + data.get("startDate") + " to " + data.get("endDate"))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10));
            }

            // Add generation date
            document.add(new Paragraph("Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.add(new Paragraph("\n"));

            // Generate content based on template
            switch (templateName) {
                case "sales-summary":
                    generateSalesSummaryPdf(document, data);
                    break;
                case "inventory-summary":
                    generateInventorySummaryPdf(document, data);
                    break;
                case "profit-loss":
                    generateProfitLossPdf(document, data);
                    break;
                case "expense-summary":
                    generateExpenseSummaryPdf(document, data);
                    break;
                default:
                    generateGenericPdf(document, data);
            }

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void generateSalesSummaryPdf(Document document, Map<String, Object> data) {
        // Summary stats
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(summaryTable, "Metric", "Value");

        addTableRow(summaryTable, "Total Sales",
                formatCurrency(data.get("totalSales")));
        addTableRow(summaryTable, "Total Orders",
                String.valueOf(data.get("totalOrders")));
        addTableRow(summaryTable, "Average Order Value",
                formatCurrency(data.get("averageOrderValue")));

        document.add(new Paragraph("Sales Summary").setBold().setFontSize(14));
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Sales by day
        if (data.containsKey("salesByDay")) {
            Table salesByDayTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}));
            salesByDayTable.setWidth(UnitValue.createPercentValue(100));

            addTableHeader(salesByDayTable, "Date", "Sales", "Orders");

            List<Object[]> salesByDay = (List<Object[]>) data.get("salesByDay");
            for (Object[] row : salesByDay) {
                addTableRow(salesByDayTable,
                        row[0].toString(),
                        formatCurrency(row[1]),
                        String.valueOf(row[2]));
            }

            document.add(new Paragraph("Sales by Day").setBold().setFontSize(14));
            document.add(salesByDayTable);
        }
    }

    private void generateInventorySummaryPdf(Document document, Map<String, Object> data) {
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(summaryTable, "Metric", "Value");

        addTableRow(summaryTable, "Total Products",
                String.valueOf(data.get("totalProducts")));
        addTableRow(summaryTable, "Low Stock Items",
                String.valueOf(data.get("lowStockItems")));
        addTableRow(summaryTable, "Out of Stock Items",
                String.valueOf(data.get("outOfStockItems")));
        addTableRow(summaryTable, "Total Inventory Value",
                formatCurrency(data.get("totalInventoryValue")));

        document.add(new Paragraph("Inventory Summary").setBold().setFontSize(14));
        document.add(summaryTable);
    }

    private void generateProfitLossPdf(Document document, Map<String, Object> data) {
        Table plTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        plTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(plTable, "Item", "Amount");

        addTableRow(plTable, "Revenue", formatCurrency(data.get("revenue")));
        addTableRow(plTable, "Expenses", formatCurrency(data.get("expenses")));
        addTableRow(plTable, "Gross Profit", formatCurrency(data.get("grossProfit")));
        addTableRow(plTable, "Profit Margin", data.get("profitMargin") + "%");

        document.add(new Paragraph("Profit & Loss Statement").setBold().setFontSize(14));
        document.add(plTable);
    }

    private void generateExpenseSummaryPdf(Document document, Map<String, Object> data) {
        // Total expenses
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        totalTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(totalTable, "Metric", "Value");
        addTableRow(totalTable, "Total Expenses",
                formatCurrency(data.get("totalExpenses")));

        document.add(new Paragraph("Expense Summary").setBold().setFontSize(14));
        document.add(totalTable);
        document.add(new Paragraph("\n"));

        // Expenses by category
        if (data.containsKey("expensesByCategory")) {
            Table categoryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            categoryTable.setWidth(UnitValue.createPercentValue(100));

            addTableHeader(categoryTable, "Category", "Amount");

            List<Object[]> expensesByCategory = (List<Object[]>) data.get("expensesByCategory");
            for (Object[] row : expensesByCategory) {
                addTableRow(categoryTable,
                        row[0].toString(),
                        formatCurrency(row[1]));
            }

            document.add(new Paragraph("Expenses by Category").setBold().setFontSize(14));
            document.add(categoryTable);
        }
    }

    private void generateGenericPdf(Document document, Map<String, Object> data) {
        Table genericTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        genericTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(genericTable, "Field", "Value");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!entry.getKey().equals("reportTitle") &&
                    !entry.getKey().equals("startDate") &&
                    !entry.getKey().equals("endDate")) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                addTableRow(genericTable, entry.getKey(), value);
            }
        }

        document.add(genericTable);
    }

    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            table.addCell(new Cell().add(new Paragraph(header).setBold()));
        }
    }

    private void addTableRow(Table table, String... values) {
        for (String value : values) {
            table.addCell(new Cell().add(new Paragraph(value)));
        }
    }

    private String formatCurrency(Object value) {
        if (value == null) return "0.00";
        if (value instanceof Double) {
            return String.format("%,.2f", (Double) value);
        } else if (value instanceof BigDecimal) {
            return String.format("%,.2f", ((BigDecimal) value).doubleValue());
        } else if (value instanceof Long) {
            return String.format("%,d", (Long) value);
        }
        return value.toString();
    }
}