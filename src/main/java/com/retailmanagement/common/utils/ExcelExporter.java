package com.retailmanagement.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExcelExporter {

    public byte[] generateExcel(Map<String, Object> data, String templateName) {
        log.debug("Generating Excel report for template: {}", templateName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(data.get("reportTitle").toString());
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // Date range
            if (data.containsKey("startDate") && data.containsKey("endDate")) {
                Row dateRow = sheet.createRow(rowNum++);
                dateRow.createCell(0).setCellValue("Period: " +
                        data.get("startDate") + " to " + data.get("endDate"));
            }

            // Generated date
            Row generatedRow = sheet.createRow(rowNum++);
            generatedRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            rowNum++; // Empty row

            // Generate report based on template
            switch (templateName) {
                case "sales-summary":
                    generateSalesSummarySheet(sheet, data, rowNum, headerStyle, currencyStyle);
                    break;
                case "inventory-summary":
                    generateInventorySummarySheet(sheet, data, rowNum, headerStyle, currencyStyle);
                    break;
                case "profit-loss":
                    generateProfitLossSheet(sheet, data, rowNum, headerStyle, currencyStyle);
                    break;
                case "expense-summary":
                    generateExpenseSummarySheet(sheet, data, rowNum, headerStyle, currencyStyle);
                    break;
                default:
                    generateGenericSheet(sheet, data, rowNum, headerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < 10; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void generateSalesSummarySheet(Sheet sheet, Map<String, Object> data,
                                           int startRow, CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = startRow;

        // Summary stats
        Row statsHeader = sheet.createRow(rowNum++);
        statsHeader.createCell(0).setCellValue("Metric");
        statsHeader.createCell(1).setCellValue("Value");
        statsHeader.getCell(0).setCellStyle(headerStyle);
        statsHeader.getCell(1).setCellStyle(headerStyle);

        Row totalSalesRow = sheet.createRow(rowNum++);
        totalSalesRow.createCell(0).setCellValue("Total Sales");
        Cell totalSalesCell = totalSalesRow.createCell(1);
        totalSalesCell.setCellValue(data.get("totalSales") != null ?
                (Double) data.get("totalSales") : 0.0);
        totalSalesCell.setCellStyle(currencyStyle);

        Row totalOrdersRow = sheet.createRow(rowNum++);
        totalOrdersRow.createCell(0).setCellValue("Total Orders");
        totalOrdersRow.createCell(1).setCellValue(data.get("totalOrders") != null ?
                ((Long) data.get("totalOrders")).doubleValue() : 0.0);

        Row avgOrderRow = sheet.createRow(rowNum++);
        avgOrderRow.createCell(0).setCellValue("Average Order Value");
        Cell avgOrderCell = avgOrderRow.createCell(1);
        avgOrderCell.setCellValue(data.get("averageOrderValue") != null ?
                (Double) data.get("averageOrderValue") : 0.0);
        avgOrderCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        // Sales by day
        if (data.containsKey("salesByDay")) {
            Row salesByDayHeader = sheet.createRow(rowNum++);
            salesByDayHeader.createCell(0).setCellValue("Date");
            salesByDayHeader.createCell(1).setCellValue("Sales");
            salesByDayHeader.createCell(2).setCellValue("Orders");
            salesByDayHeader.getCell(0).setCellStyle(headerStyle);
            salesByDayHeader.getCell(1).setCellStyle(headerStyle);
            salesByDayHeader.getCell(2).setCellStyle(headerStyle);

            List<Object[]> salesByDay = (List<Object[]>) data.get("salesByDay");
            for (Object[] row : salesByDay) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row[0].toString());
                Cell amountCell = dataRow.createCell(1);
                amountCell.setCellValue((Double) row[1]);
                amountCell.setCellStyle(currencyStyle);
                dataRow.createCell(2).setCellValue((Long) row[2]);
            }
        }
    }

    private void generateInventorySummarySheet(Sheet sheet, Map<String, Object> data,
                                               int startRow, CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = startRow;

        // Summary stats
        Row statsHeader = sheet.createRow(rowNum++);
        statsHeader.createCell(0).setCellValue("Metric");
        statsHeader.createCell(1).setCellValue("Value");
        statsHeader.getCell(0).setCellStyle(headerStyle);
        statsHeader.getCell(1).setCellStyle(headerStyle);

        Row totalProductsRow = sheet.createRow(rowNum++);
        totalProductsRow.createCell(0).setCellValue("Total Products");
        totalProductsRow.createCell(1).setCellValue(data.get("totalProducts") != null ?
                ((Long) data.get("totalProducts")).doubleValue() : 0.0);

        Row lowStockRow = sheet.createRow(rowNum++);
        lowStockRow.createCell(0).setCellValue("Low Stock Items");
        lowStockRow.createCell(1).setCellValue(data.get("lowStockItems") != null ?
                ((Long) data.get("lowStockItems")).doubleValue() : 0.0);

        Row outOfStockRow = sheet.createRow(rowNum++);
        outOfStockRow.createCell(0).setCellValue("Out of Stock Items");
        outOfStockRow.createCell(1).setCellValue(data.get("outOfStockItems") != null ?
                ((Long) data.get("outOfStockItems")).doubleValue() : 0.0);

        Row totalValueRow = sheet.createRow(rowNum++);
        totalValueRow.createCell(0).setCellValue("Total Inventory Value");
        Cell totalValueCell = totalValueRow.createCell(1);
        totalValueCell.setCellValue(data.get("totalInventoryValue") != null ?
                (Double) data.get("totalInventoryValue") : 0.0);
        totalValueCell.setCellStyle(currencyStyle);
    }

    private void generateProfitLossSheet(Sheet sheet, Map<String, Object> data,
                                         int startRow, CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = startRow;

        // Revenue
        Row revenueHeader = sheet.createRow(rowNum++);
        revenueHeader.createCell(0).setCellValue("Revenue");
        revenueHeader.getCell(0).setCellStyle(headerStyle);

        Row revenueRow = sheet.createRow(rowNum++);
        revenueRow.createCell(0).setCellValue("Total Revenue");
        Cell revenueCell = revenueRow.createCell(1);
        revenueCell.setCellValue(data.get("revenue") != null ? (Double) data.get("revenue") : 0.0);
        revenueCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        // Expenses
        Row expensesHeader = sheet.createRow(rowNum++);
        expensesHeader.createCell(0).setCellValue("Expenses");
        expensesHeader.getCell(0).setCellStyle(headerStyle);

        Row expensesRow = sheet.createRow(rowNum++);
        expensesRow.createCell(0).setCellValue("Total Expenses");
        Cell expensesCell = expensesRow.createCell(1);
        expensesCell.setCellValue(data.get("expenses") != null ? (Double) data.get("expenses") : 0.0);
        expensesCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        // Profit
        Row grossProfitRow = sheet.createRow(rowNum++);
        grossProfitRow.createCell(0).setCellValue("Gross Profit");
        Cell grossProfitCell = grossProfitRow.createCell(1);
        grossProfitCell.setCellValue(data.get("grossProfit") != null ? (Double) data.get("grossProfit") : 0.0);
        grossProfitCell.setCellStyle(currencyStyle);

        Row marginRow = sheet.createRow(rowNum++);
        marginRow.createCell(0).setCellValue("Profit Margin");
        marginRow.createCell(1).setCellValue(data.get("profitMargin") + "%");
    }

    private void generateExpenseSummarySheet(Sheet sheet, Map<String, Object> data,
                                             int startRow, CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = startRow;

        // Total expenses
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("Total Expenses");
        Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(data.get("totalExpenses") != null ?
                ((java.math.BigDecimal) data.get("totalExpenses")).doubleValue() : 0.0);
        totalCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        // Expenses by category
        if (data.containsKey("expensesByCategory")) {
            Row categoryHeader = sheet.createRow(rowNum++);
            categoryHeader.createCell(0).setCellValue("Category");
            categoryHeader.createCell(1).setCellValue("Amount");
            categoryHeader.getCell(0).setCellStyle(headerStyle);
            categoryHeader.getCell(1).setCellStyle(headerStyle);

            List<Object[]> expensesByCategory = (List<Object[]>) data.get("expensesByCategory");
            for (Object[] row : expensesByCategory) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row[0].toString());
                Cell amountCell = dataRow.createCell(1);
                amountCell.setCellValue(((java.math.BigDecimal) row[1]).doubleValue());
                amountCell.setCellStyle(currencyStyle);
            }
        }
    }

    private void generateGenericSheet(Sheet sheet, Map<String, Object> data,
                                      int startRow, CellStyle headerStyle) {
        int rowNum = startRow;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());

            if (entry.getValue() != null) {
                row.createCell(1).setCellValue(entry.getValue().toString());
            } else {
                row.createCell(1).setCellValue("");
            }
        }
    }
}