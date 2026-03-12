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
import com.retailmanagement.modules.sales.dto.response.InvoiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PdfGenerator {

    private static final float INVOICE_MARGIN = 36;

    /**
     * Generate invoice PDF from invoice response data
     */
    public byte[] generateInvoicePdf(InvoiceResponse invoice) {
        log.info("Generating invoice PDF for invoice number: {}", invoice.getInvoiceNumber());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Set document margins
            document.setMargins(INVOICE_MARGIN, INVOICE_MARGIN, INVOICE_MARGIN, INVOICE_MARGIN);

            // Add company header
            addCompanyHeader(document);

            // Add invoice title and details
            addInvoiceHeader(document, invoice);

            // Add customer details
            addCustomerDetails(document, invoice);

            // Add items table
            addInvoiceItems(document, invoice);

            // Add summary section
            addInvoiceSummary(document, invoice);

            // Add footer with terms
            addInvoiceFooter(document, invoice);

            document.close();

            log.info("Invoice PDF generated successfully for invoice: {}", invoice.getInvoiceNumber());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate invoice PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addCompanyHeader(Document document) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // Company name
        Paragraph companyName = new Paragraph("RETAIL MANAGEMENT SYSTEMS")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyName);

        // Company address
        Paragraph address = new Paragraph("123 Business Park, Commercial Street\nCity - 400001, State\nGST: 27AAABC1234A1Z5")
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(address);

        // Contact info
        Paragraph contact = new Paragraph("Tel: +91 98765 43210 | Email: accounts@retailmanagement.com")
                .setFont(normalFont)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(contact);

        // Add separator line
        document.add(new Paragraph("\n"));
    }

    private void addInvoiceHeader(Document document, InvoiceResponse invoice) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        headerTable.setWidth(UnitValue.createPercentValue(100));

        // Left side - Invoice title
        Paragraph titlePara = new Paragraph()
                .add(new Paragraph("TAX INVOICE").setFont(boldFont).setFontSize(16));
        Cell titleCell = new Cell().add(titlePara).setBorder(null);
        headerTable.addCell(titleCell);

        // Right side - Invoice details
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
        detailsTable.setWidth(UnitValue.createPercentValue(100));

        // Invoice Number
        detailsTable.addCell(new Cell().add(new Paragraph("Invoice No:").setFont(boldFont)).setBorder(null));
        detailsTable.addCell(new Cell().add(new Paragraph(invoice.getInvoiceNumber()).setFont(normalFont)).setBorder(null));

        // Date
        detailsTable.addCell(new Cell().add(new Paragraph("Date:").setFont(boldFont)).setBorder(null));
        detailsTable.addCell(new Cell().add(new Paragraph(
                invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
        ).setFont(normalFont)).setBorder(null));

        // Due Date
        if (invoice.getDueDate() != null) {
            detailsTable.addCell(new Cell().add(new Paragraph("Due Date:").setFont(boldFont)).setBorder(null));
            detailsTable.addCell(new Cell().add(new Paragraph(
                    invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            ).setFont(normalFont)).setBorder(null));
        }

        Cell detailsCell = new Cell().add(detailsTable).setBorder(null);
        headerTable.addCell(detailsCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    private void addCustomerDetails(Document document, InvoiceResponse invoice) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        Table customerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        customerTable.setWidth(UnitValue.createPercentValue(100));

        // Billing Address
        Cell billingCell = new Cell().setBorder(null);
        billingCell.add(new Paragraph("Bill To:").setFont(boldFont));
        billingCell.add(new Paragraph(invoice.getCustomerName() != null ? invoice.getCustomerName() : "Walk-in Customer").setFont(normalFont));

        if (invoice.getCustomerAddress() != null) {
            billingCell.add(new Paragraph(invoice.getCustomerAddress()).setFont(normalFont));
        }
        if (invoice.getCustomerPhone() != null) {
            billingCell.add(new Paragraph("Phone: " + invoice.getCustomerPhone()).setFont(normalFont));
        }
        if (invoice.getCustomerEmail() != null) {
            billingCell.add(new Paragraph("Email: " + invoice.getCustomerEmail()).setFont(normalFont));
        }
        if (invoice.getCustomerGst() != null) {
            billingCell.add(new Paragraph("GST: " + invoice.getCustomerGst()).setFont(normalFont));
        }
        customerTable.addCell(billingCell);

        // Shipping Address (if different)
        Cell shippingCell = new Cell().setBorder(null);
        shippingCell.add(new Paragraph("Ship To:").setFont(boldFont));
        shippingCell.add(new Paragraph(invoice.getCustomerName() != null ? invoice.getCustomerName() : "Walk-in Customer").setFont(normalFont));
        // Add shipping address if available, otherwise same as billing
        shippingCell.add(new Paragraph("Same as Billing Address").setFont(normalFont).setFontSize(9));
        customerTable.addCell(shippingCell);

        document.add(customerTable);
        document.add(new Paragraph("\n"));
    }

    private void addInvoiceItems(Document document, InvoiceResponse invoice) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        Table itemTable = new Table(UnitValue.createPercentArray(new float[]{5, 30, 10, 10, 10, 15, 20}));
        itemTable.setWidth(UnitValue.createPercentValue(100));

        // Table Headers
        addTableHeader(itemTable, boldFont, "#", "Product", "HSN", "Qty", "Price", "Discount", "Amount");

        // Table Items
        int srNo = 1;
        for (InvoiceResponse.InvoiceItemDTO item : invoice.getItems()) {
            itemTable.addCell(new Cell().add(new Paragraph(String.valueOf(srNo++)).setFont(normalFont)));
            itemTable.addCell(new Cell().add(new Paragraph(item.getProductName()).setFont(normalFont)));
            itemTable.addCell(new Cell().add(new Paragraph(item.getProductSku() != null ? item.getProductSku() : "-").setFont(normalFont)));
            itemTable.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity())).setFont(normalFont)));
            itemTable.addCell(new Cell().add(new Paragraph(formatCurrency(item.getUnitPrice())).setFont(normalFont)));

            String discount = item.getDiscount() != null && item.getDiscount().compareTo(BigDecimal.ZERO) > 0
                    ? formatCurrency(item.getDiscount()) : "-";
            itemTable.addCell(new Cell().add(new Paragraph(discount).setFont(normalFont)));

            itemTable.addCell(new Cell().add(new Paragraph(formatCurrency(item.getTotal())).setFont(normalFont)));
        }

        document.add(itemTable);
        document.add(new Paragraph("\n"));
    }

    private void addInvoiceSummary(Document document, InvoiceResponse invoice) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        // Left side - Empty or additional notes
        Cell leftCell = new Cell().setBorder(null);
        leftCell.add(new Paragraph("Amount in words:").setFont(boldFont));
        leftCell.add(new Paragraph(convertToWords(invoice.getTotalAmount()) + " Only").setFont(normalFont).setFontSize(9));
        summaryTable.addCell(leftCell);

        // Right side - Amount summary
        Table amountTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        amountTable.setWidth(UnitValue.createPercentValue(100));

        // Subtotal
        amountTable.addCell(new Cell().add(new Paragraph("Subtotal:").setFont(normalFont)).setBorder(null));
        amountTable.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getSubtotal())).setFont(normalFont)).setBorder(null));

        // Discount
        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            amountTable.addCell(new Cell().add(new Paragraph("Discount:").setFont(normalFont)).setBorder(null));
            amountTable.addCell(new Cell().add(new Paragraph("-" + formatCurrency(invoice.getDiscountAmount())).setFont(normalFont)).setBorder(null));
        }

        // Tax
        if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            amountTable.addCell(new Cell().add(new Paragraph("Tax:").setFont(normalFont)).setBorder(null));
            amountTable.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getTaxAmount())).setFont(normalFont)).setBorder(null));
        }

        // Total
        amountTable.addCell(new Cell().add(new Paragraph("Total:").setFont(boldFont)).setBorder(null));
        amountTable.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getTotalAmount())).setFont(boldFont)).setBorder(null));

        // Paid Amount
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            amountTable.addCell(new Cell().add(new Paragraph("Paid:").setFont(normalFont)).setBorder(null));
            amountTable.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getPaidAmount())).setFont(normalFont)).setBorder(null));
        }

        // Balance Due
        if (invoice.getBalanceDue() != null && invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            amountTable.addCell(new Cell().add(new Paragraph("Balance Due:").setFont(boldFont)).setBorder(null));
            amountTable.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getBalanceDue())).setFont(boldFont)).setBorder(null));
        }

        Cell rightCell = new Cell().add(amountTable).setBorder(null);
        summaryTable.addCell(rightCell);

        document.add(summaryTable);
        document.add(new Paragraph("\n"));
    }

    private void addInvoiceFooter(Document document, InvoiceResponse invoice) throws IOException {
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        // Terms and Conditions
        if (invoice.getTermsAndConditions() != null) {
            document.add(new Paragraph("Terms and Conditions:").setFont(boldFont).setFontSize(10));
            document.add(new Paragraph(invoice.getTermsAndConditions()).setFont(normalFont).setFontSize(8));
        }

        // Bank Details
        if (invoice.getBankDetails() != null) {
            document.add(new Paragraph("Bank Details:").setFont(boldFont).setFontSize(10));
            document.add(new Paragraph(invoice.getBankDetails()).setFont(normalFont).setFontSize(8));
        }

        // Declaration
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "This is a computer generated invoice and does not require a physical signature."
        ).setFont(normalFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));

        // Generated date
        document.add(new Paragraph(
                "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
        ).setFont(normalFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
    }

    private void addTableHeader(Table table, PdfFont font, String... headers) {
        for (String header : headers) {
            table.addCell(new Cell().add(new Paragraph(header).setFont(font)));
        }
    }

    private String convertToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Rupees";
        }

        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        String rupeesInWords = NumberToWords.convert(rupees) + " Rupees";
        String paiseInWords = paise > 0 ? " and " + NumberToWords.convert(paise) + " Paise" : "";

        return rupeesInWords + paiseInWords;
    }

    // Inner class for number to words conversion
    private static class NumberToWords {
        private static final String[] units = {
                "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"
        };

        private static final String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        };

        public static String convert(long n) {
            if (n < 0) {
                return "Minus " + convert(-n);
            }
            if (n == 0) {
                return "";
            }
            if (n < 20) {
                return units[(int) n];
            }
            if (n < 100) {
                return tens[(int) n / 10] + ((n % 10 != 0) ? " " + units[(int) n % 10] : "");
            }
            if (n < 1000) {
                return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " and " + convert(n % 100) : "");
            }
            if (n < 100000) {
                return convert(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convert(n % 1000) : "");
            }
            if (n < 10000000) {
                return convert(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convert(n % 100000) : "");
            }
            return convert(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convert(n % 10000000) : "");
        }
    }

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