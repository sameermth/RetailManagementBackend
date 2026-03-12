package com.retailmanagement.modules.report.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.common.utils.ExcelExporter;
import com.retailmanagement.common.utils.PdfGenerator;
import com.retailmanagement.modules.customer.repository.CustomerDueRepository;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.inventory.repository.InventoryRepository;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.purchase.repository.PurchaseRepository;
import com.retailmanagement.modules.report.dto.request.ReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportResponse;
import com.retailmanagement.modules.report.dto.response.ReportSummaryResponse;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.model.Report;
import com.retailmanagement.modules.report.repository.ReportRepository;
import com.retailmanagement.modules.report.service.ReportGeneratorService;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDueRepository dueRepository;
    private final ExpenseRepository expenseRepository;
    private final PdfGenerator pdfGenerator;
    private final ExcelExporter excelExporter;

    @Override
    @Async
    public ReportResponse generateReport(ReportRequest request, Long userId) {
        log.info("Generating report of type: {} for user: {}", request.getReportType(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Create report record
        Report report = Report.builder()
                .reportId(generateReportId())
                .reportName(request.getReportName() != null ? request.getReportName() :
                        request.getReportType().toString() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
                .reportType(request.getReportType())
                .format(request.getFormat() != null ? request.getFormat() : ReportFormat.PDF)
                .generatedBy(user)
                .generatedDate(LocalDateTime.now())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .parameters(request.getParameters() != null ? request.getParameters() : new HashMap<>())
                .status("GENERATING")
                .build();

        Report savedReport = reportRepository.save(report);

        // Generate report asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] reportData = generateReportData(request);
                String fileUrl = saveReportFile(reportData, savedReport);

                savedReport.setFileUrl(fileUrl);
                savedReport.setFileSize((long) reportData.length);
                savedReport.setStatus("COMPLETED");
                reportRepository.save(savedReport);

                log.info("Report generated successfully with ID: {}", savedReport.getReportId());
                return savedReport;
            } catch (Exception e) {
                log.error("Failed to generate report: {}", e.getMessage(), e);
                savedReport.setStatus("FAILED");
                savedReport.setErrorMessage(e.getMessage());
                reportRepository.save(savedReport);
                return savedReport;
            }
        });

        return convertToResponse(savedReport);
    }

    private String generateReportId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String reportId = "RPT-" + timestamp + "-" + randomPart;

        while (reportRepository.existsByReportId(reportId)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            reportId = "RPT-" + timestamp + "-" + randomPart;
        }

        return reportId;
    }

    private byte[] generateReportData(ReportRequest request) {
        switch (request.getReportType()) {
            case SALES_SUMMARY:
            case SALES_DETAILED:
            case SALES_BY_PRODUCT:
            case SALES_BY_CATEGORY:
            case SALES_BY_CUSTOMER:
            case TOP_PRODUCTS:
            case TOP_CUSTOMERS:
                return generateSalesReport(request);

            case INVENTORY_SUMMARY:
            case INVENTORY_DETAILED:
            case LOW_STOCK_REPORT:
            case STOCK_MOVEMENT:
            case INVENTORY_VALUATION:
                return generateInventoryReport(request);

            case PURCHASE_SUMMARY:
            case PURCHASE_DETAILED:
            case PURCHASE_BY_SUPPLIER:
            case PURCHASE_BY_PRODUCT:
                return generatePurchaseReport(request);

            case PROFIT_LOSS:
            case EXPENSE_SUMMARY:
            case EXPENSE_DETAILED:
            case EXPENSE_BY_CATEGORY:
            case REVENUE_REPORT:
                return generateFinancialReport(request);

            case CUSTOMER_DUES:
            case CUSTOMER_PAYMENTS:
            case CUSTOMER_LIFETIME_VALUE:
                return generateCustomerReport(request);

            case TAX_REPORT:
                return generateTaxReport(request);

            default:
                throw new BusinessException("Unsupported report type: " + request.getReportType());
        }
    }

    private String saveReportFile(byte[] data, Report report) {
        // In real implementation, save to file system or cloud storage
        // Return the URL/path
        String fileName = report.getReportId() + "." + report.getFormat().toString().toLowerCase();
        return "/reports/" + fileName;
    }

    @Override
    public ReportResponse getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        return convertToResponse(report);
    }

    @Override
    public ReportResponse getReportByReportId(String reportId) {
        Report report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        return convertToResponse(report);
    }

    @Override
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Override
    public List<ReportResponse> getReportsByType(ReportType reportType) {
        return reportRepository.findByReportType(reportType).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public Page<ReportResponse> getReportsByType(ReportType reportType, Pageable pageable) {
        return reportRepository.findByReportType(reportType, pageable)
                .map(this::convertToResponse);
    }

    @Override
    public List<ReportResponse> getReportsByUser(Long userId) {
        return reportRepository.findByGeneratedByUserId(userId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable) {
        return reportRepository.findByGeneratedByUserId(userId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    public List<ReportResponse> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.findByGeneratedDateBetween(startDate, endDate).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        // Delete physical file if exists
        // fileStorageService.delete(report.getFileUrl());

        reportRepository.delete(report);
    }

    @Override
    public byte[] downloadReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        if (report.getStatus().equals("FAILED")) {
            throw new BusinessException("Cannot download failed report");
        }

        incrementDownloadCount(id);

        // In real implementation, read file from storage
        // return fileStorageService.read(report.getFileUrl());
        return new byte[0]; // Placeholder
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        report.setDownloadCount(report.getDownloadCount() + 1);
        reportRepository.save(report);
    }

    @Override
    public ReportSummaryResponse getDashboardSummary() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);
        LocalDateTime startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay();

        // Sales Summary
        Double totalSales = saleRepository.getTotalSalesForPeriod(startOfMonth, endOfMonth);
        Long totalOrders = saleRepository.countSalesForPeriod(startOfMonth, endOfMonth);
        Double avgOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0;

        // Inventory Summary
        Long totalProducts = productRepository.count();
        Long lowStockItems = inventoryRepository.countLowStock();
        Long outOfStockItems = inventoryRepository.countOutOfStock();
        Double totalInventoryValue = calculateTotalInventoryValue();

        // Financial Summary
        Double totalExpenses = expenseRepository.getTotalExpensesForPeriod(startOfMonth, endOfMonth).doubleValue();
        Double profit = totalSales - totalExpenses;
        Double profitMargin = totalSales > 0 ? (profit / totalSales) * 100 : 0;

        // Customer Summary
        Long totalCustomers = customerRepository.count();
        Long newCustomers = customerRepository.countByCreatedDate(LocalDate.now());
        Double totalDues = dueRepository.getTotalDueAmount().doubleValue();
        Long overdueCount = (long) dueRepository.findOverdueDues(LocalDate.now()).size();

        return ReportSummaryResponse.builder()
                .salesSummary(ReportSummaryResponse.SalesSummary.builder()
                        .totalSales(totalSales)
                        .totalOrders(totalOrders)
                        .averageOrderValue(avgOrderValue)
                        .build())
                .inventorySummary(ReportSummaryResponse.InventorySummary.builder()
                        .totalProducts(totalProducts)
                        .lowStockItems(lowStockItems)
                        .outOfStockItems(outOfStockItems)
                        .totalInventoryValue(totalInventoryValue)
                        .build())
                .financialSummary(ReportSummaryResponse.FinancialSummary.builder()
                        .revenue(totalSales)
                        .expenses(totalExpenses)
                        .profit(profit)
                        .profitMargin(profitMargin)
                        .build())
                .customerSummary(ReportSummaryResponse.CustomerSummary.builder()
                        .totalCustomers(totalCustomers)
                        .newCustomers(newCustomers)
                        .totalDues(totalDues)
                        .overdueCount(overdueCount)
                        .build())
                .build();
    }

    private Double calculateTotalInventoryValue() {
        // Implementation to calculate total inventory value
        // This would sum (quantity * average cost) for all inventory items
        return 0.0; // Placeholder
    }

    @Override
    public byte[] generateSalesReport(ReportRequest request) {
        log.debug("Generating sales report: {}", request.getReportType());

        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate() : LocalDateTime.now();

        switch (request.getReportType()) {
            case SALES_SUMMARY:
                return generateSalesSummaryReport(startDate, endDate, request.getFormat());
            case SALES_DETAILED:
                return generateSalesDetailedReport(startDate, endDate, request.getFormat());
            case SALES_BY_PRODUCT:
                return generateSalesByProductReport(startDate, endDate, request.getFormat());
            case SALES_BY_CATEGORY:
                return generateSalesByCategoryReport(startDate, endDate, request.getFormat());
            case SALES_BY_CUSTOMER:
                return generateSalesByCustomerReport(startDate, endDate, request.getFormat());
            case TOP_PRODUCTS:
                return generateTopProductsReport(startDate, endDate, request.getFormat());
            default:
                throw new BusinessException("Unsupported sales report type");
        }
    }

    private byte[] generateSalesSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        // Gather data
        Double totalSales = saleRepository.getTotalSalesForPeriod(startDate, endDate);
        Long totalOrders = saleRepository.countSalesForPeriod(startDate, endDate);
        Double avgOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0;

        // Get sales by day
        List<Object[]> salesByDay = new ArrayList<>(); // Fetch from repository

        // Get sales by payment method
        List<Object[]> salesByPaymentMethod = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalSales", totalSales);
        data.put("totalOrders", totalOrders);
        data.put("averageOrderValue", avgOrderValue);
        data.put("salesByDay", salesByDay);
        data.put("salesByPaymentMethod", salesByPaymentMethod);

        return generateReportData(data, format, "sales-summary");
    }

    private byte[] generateSalesDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> sales = new ArrayList<>(); // Fetch detailed sales data from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Sales Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("sales", sales);

        return generateReportData(data, format, "sales-detailed");
    }

    private byte[] generateSalesByProductReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> salesByProduct = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Product Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByProduct", salesByProduct);

        return generateReportData(data, format, "sales-by-product");
    }

    private byte[] generateSalesByCategoryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> salesByCategory = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Category Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByCategory", salesByCategory);

        return generateReportData(data, format, "sales-by-category");
    }

    private byte[] generateSalesByCustomerReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> salesByCustomer = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Customer Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByCustomer", salesByCustomer);

        return generateReportData(data, format, "sales-by-customer");
    }

    private byte[] generateTopProductsReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> topProducts = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Top Products Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("topProducts", topProducts);

        return generateReportData(data, format, "top-products");
    }

    @Override
    public byte[] generateInventoryReport(ReportRequest request) {
        log.debug("Generating inventory report: {}", request.getReportType());

        LocalDateTime asOfDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        switch (request.getReportType()) {
            case INVENTORY_SUMMARY:
                return generateInventorySummaryReport(asOfDate, request.getFormat());
            case INVENTORY_DETAILED:
                return generateInventoryDetailedReport(asOfDate, request.getFormat());
            case LOW_STOCK_REPORT:
                return generateLowStockReport(request.getFormat());
            case INVENTORY_VALUATION:
                return generateInventoryValuationReport(asOfDate, request.getFormat());
            default:
                throw new BusinessException("Unsupported inventory report type");
        }
    }

    private byte[] generateInventorySummaryReport(LocalDateTime asOfDate, ReportFormat format) {
        Long totalProducts = productRepository.count();
        Long lowStockItems = inventoryRepository.countLowStock();
        Long outOfStockItems = inventoryRepository.countOutOfStock();
        Double totalValue = calculateTotalInventoryValue();

        List<Object[]> stockByCategory = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Inventory Summary Report");
        data.put("asOfDate", asOfDate);
        data.put("totalProducts", totalProducts);
        data.put("lowStockItems", lowStockItems);
        data.put("outOfStockItems", outOfStockItems);
        data.put("totalInventoryValue", totalValue);
        data.put("stockByCategory", stockByCategory);

        return generateReportData(data, format, "inventory-summary");
    }

    private byte[] generateInventoryDetailedReport(LocalDateTime asOfDate, ReportFormat format) {
        List<Object[]> inventory = new ArrayList<>(); // Fetch detailed inventory data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Inventory Report");
        data.put("asOfDate", asOfDate);
        data.put("inventory", inventory);

        return generateReportData(data, format, "inventory-detailed");
    }

    private byte[] generateLowStockReport(ReportFormat format) {
        List<Object[]> lowStockItems = new ArrayList<>(); // Fetch low stock items

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Low Stock Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("lowStockItems", lowStockItems);

        return generateReportData(data, format, "low-stock");
    }

    private byte[] generateInventoryValuationReport(LocalDateTime asOfDate, ReportFormat format) {
        List<Object[]> valuation = new ArrayList<>(); // Fetch inventory valuation data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Inventory Valuation Report");
        data.put("asOfDate", asOfDate);
        data.put("valuation", valuation);

        return generateReportData(data, format, "inventory-valuation");
    }

    @Override
    public byte[] generatePurchaseReport(ReportRequest request) {
        log.debug("Generating purchase report: {}", request.getReportType());

        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate() : LocalDateTime.now();

        switch (request.getReportType()) {
            case PURCHASE_SUMMARY:
                return generatePurchaseSummaryReport(startDate, endDate, request.getFormat());
            case PURCHASE_DETAILED:
                return generatePurchaseDetailedReport(startDate, endDate, request.getFormat());
            case PURCHASE_BY_SUPPLIER:
                return generatePurchaseBySupplierReport(startDate, endDate, request.getFormat());
            default:
                throw new BusinessException("Unsupported purchase report type");
        }
    }

    private byte[] generatePurchaseSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Double totalPurchases = purchaseRepository.getTotalPurchaseAmountForPeriod(startDate, endDate);
        Long pendingApproval = purchaseRepository.countPendingApproval();

        List<Object[]> purchasesBySupplier = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Purchase Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalPurchases", totalPurchases);
        data.put("pendingApproval", pendingApproval);
        data.put("purchasesBySupplier", purchasesBySupplier);

        return generateReportData(data, format, "purchase-summary");
    }

    private byte[] generatePurchaseDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> purchases = new ArrayList<>(); // Fetch detailed purchase data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Purchase Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("purchases", purchases);

        return generateReportData(data, format, "purchase-detailed");
    }

    private byte[] generatePurchaseBySupplierReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> purchasesBySupplier = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Purchases by Supplier Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("purchasesBySupplier", purchasesBySupplier);

        return generateReportData(data, format, "purchase-by-supplier");
    }

    @Override
    public byte[] generateFinancialReport(ReportRequest request) {
        log.debug("Generating financial report: {}", request.getReportType());

        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate() : LocalDateTime.now();

        switch (request.getReportType()) {
            case PROFIT_LOSS:
                return generateProfitLossReport(startDate, endDate, request.getFormat());
            case EXPENSE_SUMMARY:
                return generateExpenseSummaryReport(startDate, endDate, request.getFormat());
            case EXPENSE_DETAILED:
                return generateExpenseDetailedReport(startDate, endDate, request.getFormat());
            case EXPENSE_BY_CATEGORY:
                return generateExpenseByCategoryReport(startDate, endDate, request.getFormat());
            case REVENUE_REPORT:
                return generateRevenueReport(startDate, endDate, request.getFormat());
            default:
                throw new BusinessException("Unsupported financial report type");
        }
    }

    private byte[] generateProfitLossReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Double revenue = saleRepository.getTotalSalesForPeriod(startDate, endDate);
        Double expenses = expenseRepository.getTotalExpensesForPeriod(startDate, endDate).doubleValue();
        Double grossProfit = revenue - expenses;
        Double profitMargin = revenue > 0 ? (grossProfit / revenue) * 100 : 0;

        List<Object[]> revenueByMonth = new ArrayList<>(); // Fetch from repository
        List<Object[]> expensesByCategory = expenseRepository.getExpensesGroupedByCategory(startDate, endDate);

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Profit & Loss Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("revenue", revenue);
        data.put("expenses", expenses);
        data.put("grossProfit", grossProfit);
        data.put("profitMargin", profitMargin);
        data.put("revenueByMonth", revenueByMonth);
        data.put("expensesByCategory", expensesByCategory);

        return generateReportData(data, format, "profit-loss");
    }

    private byte[] generateExpenseSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesForPeriod(startDate, endDate);
        List<Object[]> expensesByCategory = expenseRepository.getExpensesGroupedByCategory(startDate, endDate);

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Expense Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalExpenses", totalExpenses);
        data.put("expensesByCategory", expensesByCategory);

        return generateReportData(data, format, "expense-summary");
    }

    private byte[] generateExpenseDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> expenses = new ArrayList<>(); // Fetch detailed expense data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Expense Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("expenses", expenses);

        return generateReportData(data, format, "expense-detailed");
    }

    private byte[] generateExpenseByCategoryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Object[]> expensesByCategory = expenseRepository.getExpensesGroupedByCategory(startDate, endDate);

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Expenses by Category Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("expensesByCategory", expensesByCategory);

        return generateReportData(data, format, "expense-by-category");
    }

    private byte[] generateRevenueReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Double totalRevenue = saleRepository.getTotalSalesForPeriod(startDate, endDate);
        List<Object[]> revenueByMonth = new ArrayList<>(); // Fetch from repository

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Revenue Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalRevenue", totalRevenue);
        data.put("revenueByMonth", revenueByMonth);

        return generateReportData(data, format, "revenue");
    }

    @Override
    public byte[] generateCustomerReport(ReportRequest request) {
        log.debug("Generating customer report: {}", request.getReportType());

        switch (request.getReportType()) {
            case CUSTOMER_DUES:
                return generateCustomerDuesReport(request.getFormat());
            case CUSTOMER_PAYMENTS:
                return generateCustomerPaymentsReport(request.getFormat());
            case CUSTOMER_LIFETIME_VALUE:
                return generateCustomerLTVReport(request.getFormat());
            default:
                throw new BusinessException("Unsupported customer report type");
        }
    }

    private byte[] generateCustomerDuesReport(ReportFormat format) {
        List<Object[]> customerDues = new ArrayList<>(); // Fetch customer dues data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Dues Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerDues", customerDues);

        return generateReportData(data, format, "customer-dues");
    }

    private byte[] generateCustomerPaymentsReport(ReportFormat format) {
        List<Object[]> customerPayments = new ArrayList<>(); // Fetch customer payments data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Payments Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerPayments", customerPayments);

        return generateReportData(data, format, "customer-payments");
    }

    private byte[] generateCustomerLTVReport(ReportFormat format) {
        List<Object[]> customerLTV = new ArrayList<>(); // Fetch customer lifetime value data

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Lifetime Value Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerLTV", customerLTV);

        return generateReportData(data, format, "customer-ltv");
    }

    @Override
    public byte[] generateTaxReport(ReportRequest request) {
        log.debug("Generating tax report");

        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate() : LocalDateTime.now();

        List<Object[]> taxData = new ArrayList<>(); // Fetch tax data from sales and purchases

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Tax Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("taxData", taxData);

        return generateReportData(data, request.getFormat(), "tax-report");
    }

    private byte[] generateReportData(Map<String, Object> data, ReportFormat format, String templateName) {
        switch (format) {
            case PDF:
                return pdfGenerator.generateReport(data, templateName);
            case EXCEL:
                return excelExporter.generateExcel(data, templateName);
            case CSV:
                return generateCsv(data);
            case HTML:
                return generateHtml(data, templateName);
            case JSON:
                return generateJson(data);
            default:
                throw new BusinessException("Unsupported report format: " + format);
        }
    }

    private byte[] generateCsv(Map<String, Object> data) {
        // Implement CSV generation
        return new byte[0];
    }

    private byte[] generateHtml(Map<String, Object> data, String templateName) {
        // Implement HTML generation
        return new byte[0];
    }

    private byte[] generateJson(Map<String, Object> data) {
        // Implement JSON generation
        return new byte[0];
    }

    private ReportResponse convertToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reportId(report.getReportId())
                .reportName(report.getReportName())
                .reportType(report.getReportType())
                .format(report.getFormat())
                .generatedBy(report.getGeneratedBy() != null ? report.getGeneratedBy().getUsername() : null)
                .generatedDate(report.getGeneratedDate())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .description(report.getDescription())
                .fileUrl(report.getFileUrl())
                .fileSize(report.getFileSize())
                .parameters(report.getParameters())
                .status(report.getStatus())
                .downloadCount(report.getDownloadCount())
                .createdAt(report.getCreatedAt())
                .build();
    }
}