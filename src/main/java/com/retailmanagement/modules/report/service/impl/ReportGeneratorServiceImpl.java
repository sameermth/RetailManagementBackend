package com.retailmanagement.modules.report.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.common.utils.ExcelExporter;
import com.retailmanagement.common.utils.PdfGenerator;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.report.dto.request.ReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportResponse;
import com.retailmanagement.modules.report.dto.response.ReportSummaryResponse;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.model.Report;
import com.retailmanagement.modules.report.repository.ReportRepository;
import com.retailmanagement.modules.report.service.ReportGeneratorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    private static final Set<String> PENDING_PURCHASE_STATUSES = Set.of("DRAFT", "OPEN", "PENDING", "SUBMITTED");
    private static final BigDecimal LOW_STOCK_THRESHOLD = BigDecimal.valueOf(5);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StoreProductRepository productRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final ExpenseRepository expenseRepository;
    private final PdfGenerator pdfGenerator;
    private final ExcelExporter excelExporter;

    @Override
    @Async
    public ReportResponse generateReport(ReportRequest request, Long userId) {
        log.info("Generating report of type: {} for user: {}", request.getReportType(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Report report = Report.builder()
                .reportId(generateReportId())
                .reportName(request.getReportName() != null ? request.getReportName()
                        : request.getReportType() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
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

        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] reportData = generateReportData(request);
                savedReport.setFileUrl(saveReportFile(reportData, savedReport));
                savedReport.setFileSize((long) reportData.length);
                savedReport.setStatus("COMPLETED");
            } catch (Exception e) {
                log.error("Failed to generate report: {}", e.getMessage(), e);
                savedReport.setStatus("FAILED");
                savedReport.setErrorMessage(e.getMessage());
            }
            return reportRepository.save(savedReport);
        });

        return convertToResponse(savedReport);
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
        return reportRepository.findAll(pageable).map(this::convertToResponse);
    }

    @Override
    public List<ReportResponse> getReportsByType(ReportType reportType) {
        return reportRepository.findByReportType(reportType).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public Page<ReportResponse> getReportsByType(ReportType reportType, Pageable pageable) {
        return reportRepository.findByReportType(reportType, pageable).map(this::convertToResponse);
    }

    @Override
    public List<ReportResponse> getReportsByUser(Long userId) {
        return reportRepository.findByGeneratedById(userId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable) {
        return reportRepository.findByGeneratedById(userId, pageable).map(this::convertToResponse);
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
        reportRepository.delete(report);
    }

    @Override
    public byte[] downloadReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        if ("FAILED".equals(report.getStatus())) {
            throw new BusinessException("Cannot download failed report");
        }
        incrementDownloadCount(id);
        return new byte[0];
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

        BigDecimal revenue = getSalesTotal(startOfMonth, endOfMonth);
        long totalOrders = getSalesInvoices(startOfMonth, endOfMonth).size();
        BigDecimal expenses = zeroIfNull(expenseRepository.getTotalExpensesForPeriod(startOfMonth.toLocalDate(), endOfMonth.toLocalDate()));
        BigDecimal profit = revenue.subtract(expenses);

        return ReportSummaryResponse.builder()
                .salesSummary(ReportSummaryResponse.SalesSummary.builder()
                        .totalSales(revenue.doubleValue())
                        .totalOrders(totalOrders)
                        .averageOrderValue(totalOrders == 0 ? 0.0 : revenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue())
                        .salesByDay(getSalesByDay(startOfMonth, endOfMonth))
                        .topProducts(getTopProductsData(startOfMonth, endOfMonth, 5))
                        .salesByPaymentMethod(getSalesByPaymentMethod(startOfMonth, endOfMonth))
                        .build())
                .inventorySummary(ReportSummaryResponse.InventorySummary.builder()
                        .totalProducts(productRepository.count())
                        .lowStockItems((long) getLowStockItems().size())
                        .outOfStockItems(countOutOfStock())
                        .totalInventoryValue(calculateTotalInventoryValue().doubleValue())
                        .stockByCategory(List.of())
                        .build())
                .financialSummary(ReportSummaryResponse.FinancialSummary.builder()
                        .revenue(revenue.doubleValue())
                        .expenses(expenses.doubleValue())
                        .profit(profit.doubleValue())
                        .profitMargin(revenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                                : profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                        .revenueByMonth(List.of())
                        .expensesByCategory(toNamedAmountMaps(
                                expenseRepository.getExpensesGroupedByCategory(startOfMonth.toLocalDate(), endOfMonth.toLocalDate()),
                                "category"
                        ))
                        .build())
                .customerSummary(ReportSummaryResponse.CustomerSummary.builder()
                        .totalCustomers(customerRepository.count())
                        .newCustomers(customerRepository.findAll().stream()
                                .map(Customer::getCreatedAt)
                                .filter(Objects::nonNull)
                                .filter(createdAt -> createdAt.toLocalDate().isEqual(LocalDate.now()))
                                .count())
                        .totalDues(getTotalOutstanding().doubleValue())
                        .overdueCount(countOverdueCustomers())
                        .topCustomers(getTopCustomers())
                        .build())
                .charts(Map.of())
                .build();
    }

    @Override
    public byte[] generateSalesReport(ReportRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        return switch (request.getReportType()) {
            case SALES_SUMMARY -> generateSalesSummaryReport(startDate, endDate, request.getFormat());
            case SALES_DETAILED -> generateSalesDetailedReport(startDate, endDate, request.getFormat());
            case SALES_BY_PRODUCT -> generateSalesByProductReport(startDate, endDate, request.getFormat());
            case SALES_BY_CATEGORY -> generateSalesByCategoryReport(startDate, endDate, request.getFormat());
            case SALES_BY_CUSTOMER -> generateSalesByCustomerReport(startDate, endDate, request.getFormat());
            case TOP_PRODUCTS -> generateTopProductsReport(startDate, endDate, request.getFormat());
            default -> throw new BusinessException("Unsupported sales report type");
        };
    }

    @Override
    public byte[] generateInventoryReport(ReportRequest request) {
        LocalDateTime asOfDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        return switch (request.getReportType()) {
            case INVENTORY_SUMMARY -> generateInventorySummaryReport(asOfDate, request.getFormat());
            case INVENTORY_DETAILED -> generateInventoryDetailedReport(asOfDate, request.getFormat());
            case LOW_STOCK_REPORT -> generateLowStockReport(request.getFormat());
            case INVENTORY_VALUATION -> generateInventoryValuationReport(asOfDate, request.getFormat());
            default -> throw new BusinessException("Unsupported inventory report type");
        };
    }

    @Override
    public byte[] generatePurchaseReport(ReportRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        return switch (request.getReportType()) {
            case PURCHASE_SUMMARY -> generatePurchaseSummaryReport(startDate, endDate, request.getFormat());
            case PURCHASE_DETAILED -> generatePurchaseDetailedReport(startDate, endDate, request.getFormat());
            case PURCHASE_BY_SUPPLIER -> generatePurchaseBySupplierReport(startDate, endDate, request.getFormat());
            default -> throw new BusinessException("Unsupported purchase report type");
        };
    }

    @Override
    public byte[] generateFinancialReport(ReportRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        return switch (request.getReportType()) {
            case PROFIT_LOSS -> generateProfitLossReport(startDate, endDate, request.getFormat());
            case EXPENSE_SUMMARY -> generateExpenseSummaryReport(startDate, endDate, request.getFormat());
            case EXPENSE_DETAILED -> generateExpenseDetailedReport(startDate, endDate, request.getFormat());
            case EXPENSE_BY_CATEGORY -> generateExpenseByCategoryReport(startDate, endDate, request.getFormat());
            case REVENUE_REPORT -> generateRevenueReport(startDate, endDate, request.getFormat());
            default -> throw new BusinessException("Unsupported financial report type");
        };
    }

    @Override
    public byte[] generateCustomerReport(ReportRequest request) {
        return switch (request.getReportType()) {
            case CUSTOMER_DUES -> generateCustomerDuesReport(request.getFormat());
            case CUSTOMER_PAYMENTS -> generateCustomerPaymentsReport(request.getFormat());
            case CUSTOMER_LIFETIME_VALUE -> generateCustomerLtvReport(request.getFormat());
            default -> throw new BusinessException("Unsupported customer report type");
        };
    }

    @Override
    public byte[] generateExpenseReport(ReportRequest request) {
        throw new UnsupportedOperationException("Use generateReport() method instead and pass userId");
    }

    @Override
    public byte[] generateTaxReport(ReportRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        List<Map<String, Object>> taxData = new ArrayList<>();
        taxData.add(Map.of(
                "source", "sales",
                "taxAmount", getSalesInvoices(startDate, endDate).stream()
                        .map(SalesInvoice::getTaxAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ));
        taxData.add(Map.of(
                "source", "purchases",
                "taxAmount", getPurchaseOrders(startDate, endDate).stream()
                        .map(PurchaseOrder::getTaxAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ));

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Tax Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("taxData", taxData);
        return renderReport(data, request.getFormat(), "tax-report");
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
        return switch (request.getReportType()) {
            case SALES_SUMMARY, SALES_DETAILED, SALES_BY_PRODUCT, SALES_BY_CATEGORY, SALES_BY_CUSTOMER, TOP_PRODUCTS, TOP_CUSTOMERS -> generateSalesReport(request);
            case INVENTORY_SUMMARY, INVENTORY_DETAILED, LOW_STOCK_REPORT, STOCK_MOVEMENT, INVENTORY_VALUATION -> generateInventoryReport(request);
            case PURCHASE_SUMMARY, PURCHASE_DETAILED, PURCHASE_BY_SUPPLIER, PURCHASE_BY_PRODUCT -> generatePurchaseReport(request);
            case PROFIT_LOSS, EXPENSE_SUMMARY, EXPENSE_DETAILED, EXPENSE_BY_CATEGORY, REVENUE_REPORT -> generateFinancialReport(request);
            case CUSTOMER_DUES, CUSTOMER_PAYMENTS, CUSTOMER_LIFETIME_VALUE -> generateCustomerReport(request);
            case TAX_REPORT -> generateTaxReport(request);
            default -> throw new BusinessException("Unsupported report type: " + request.getReportType());
        };
    }

    private String saveReportFile(byte[] data, Report report) {
        return "/reports/" + report.getReportId() + "." + report.getFormat().toString().toLowerCase();
    }

    private byte[] generateSalesSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalSales", getSalesTotal(startDate, endDate));
        data.put("totalOrders", getSalesInvoices(startDate, endDate).size());
        data.put("averageOrderValue", getAverageOrderValue(startDate, endDate));
        data.put("salesByDay", getSalesByDay(startDate, endDate));
        data.put("salesByPaymentMethod", getSalesByPaymentMethod(startDate, endDate));
        return renderReport(data, format, "sales-summary");
    }

    private byte[] generateSalesDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Map<String, Object>> sales = getSalesInvoices(startDate, endDate).stream()
                .map(invoice -> Map.<String, Object>of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "invoiceDate", invoice.getInvoiceDate(),
                        "customerId", invoice.getCustomerId(),
                        "status", invoice.getStatus(),
                        "subtotal", zeroIfNull(invoice.getSubtotal()),
                        "taxAmount", zeroIfNull(invoice.getTaxAmount()),
                        "totalAmount", zeroIfNull(invoice.getTotalAmount())
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Sales Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("sales", sales);
        return renderReport(data, format, "sales-detailed");
    }

    private byte[] generateSalesByProductReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Product Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByProduct", getTopProductsData(startDate, endDate, Integer.MAX_VALUE));
        return renderReport(data, format, "sales-by-product");
    }

    private byte[] generateSalesByCategoryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Category Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByCategory", List.of());
        return renderReport(data, format, "sales-by-category");
    }

    private byte[] generateSalesByCustomerReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<Long, BigDecimal> salesByCustomer = getSalesInvoices(startDate, endDate).stream()
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        List<Map<String, Object>> rows = salesByCustomer.entrySet().stream()
                .map(entry -> {
                    Customer customer = customers.get(entry.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("customerId", entry.getKey());
                    row.put("customerName", customer != null ? customer.getFullName() : "Unknown Customer");
                    row.put("amount", entry.getValue());
                    return row;
                })
                .sorted((left, right) -> ((BigDecimal) right.get("amount")).compareTo((BigDecimal) left.get("amount")))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Sales by Customer Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("salesByCustomer", rows);
        return renderReport(data, format, "sales-by-customer");
    }

    private byte[] generateTopProductsReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Top Products Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("topProducts", getTopProductsData(startDate, endDate, 10));
        return renderReport(data, format, "top-products");
    }

    private byte[] generateInventorySummaryReport(LocalDateTime asOfDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Inventory Summary Report");
        data.put("asOfDate", asOfDate);
        data.put("totalProducts", productRepository.count());
        data.put("lowStockItems", getLowStockItems().size());
        data.put("outOfStockItems", countOutOfStock());
        data.put("totalInventoryValue", calculateTotalInventoryValue());
        data.put("stockByCategory", List.of());
        return renderReport(data, format, "inventory-summary");
    }

    private byte[] generateInventoryDetailedReport(LocalDateTime asOfDate, ReportFormat format) {
        Map<Long, StoreProduct> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));
        List<Map<String, Object>> inventoryRows = inventoryBalanceRepository.findAll().stream()
                .map(balance -> {
                    StoreProduct product = products.get(balance.getProductId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", balance.getProductId());
                    row.put("productName", product != null ? product.getName() : "Unknown Product");
                    row.put("sku", product != null ? product.getSku() : null);
                    row.put("warehouseId", balance.getWarehouseId());
                    row.put("availableQuantity", zeroIfNull(balance.getAvailableBaseQuantity()));
                    row.put("onHandQuantity", zeroIfNull(balance.getOnHandBaseQuantity()));
                    row.put("avgCost", zeroIfNull(balance.getAvgCost()));
                    return row;
                })
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Inventory Report");
        data.put("asOfDate", asOfDate);
        data.put("inventory", inventoryRows);
        return renderReport(data, format, "inventory-detailed");
    }

    private byte[] generateLowStockReport(ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Low Stock Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("lowStockItems", getLowStockItems());
        return renderReport(data, format, "low-stock");
    }

    private byte[] generateInventoryValuationReport(LocalDateTime asOfDate, ReportFormat format) {
        List<Map<String, Object>> valuation = inventoryBalanceRepository.findAll().stream()
                .map(balance -> Map.<String, Object>of(
                        "productId", balance.getProductId(),
                        "warehouseId", balance.getWarehouseId(),
                        "availableQuantity", zeroIfNull(balance.getAvailableBaseQuantity()),
                        "avgCost", zeroIfNull(balance.getAvgCost()),
                        "value", zeroIfNull(balance.getAvailableBaseQuantity()).multiply(zeroIfNull(balance.getAvgCost()))
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Inventory Valuation Report");
        data.put("asOfDate", asOfDate);
        data.put("valuation", valuation);
        return renderReport(data, format, "inventory-valuation");
    }

    private byte[] generatePurchaseSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<PurchaseOrder> purchases = getPurchaseOrders(startDate, endDate);
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Purchase Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalPurchases", purchases.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("pendingApproval", purchases.stream().filter(this::isPendingPurchaseOrder).count());
        data.put("purchasesBySupplier", List.of());
        return renderReport(data, format, "purchase-summary");
    }

    private byte[] generatePurchaseDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Map<String, Object>> purchases = getPurchaseOrders(startDate, endDate).stream()
                .map(order -> Map.<String, Object>of(
                        "poNumber", order.getPoNumber(),
                        "poDate", order.getPoDate(),
                        "supplierId", order.getSupplierId(),
                        "status", order.getStatus(),
                        "totalAmount", zeroIfNull(order.getTotalAmount())
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Purchase Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("purchases", purchases);
        return renderReport(data, format, "purchase-detailed");
    }

    private byte[] generatePurchaseBySupplierReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        List<Map<String, Object>> purchasesBySupplier = getPurchaseOrders(startDate, endDate).stream()
                .collect(Collectors.groupingBy(
                        PurchaseOrder::getSupplierId,
                        Collectors.mapping(
                                order -> zeroIfNull(order.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("supplierId", entry.getKey(), "amount", entry.getValue()))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Purchases by Supplier Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("purchasesBySupplier", purchasesBySupplier);
        return renderReport(data, format, "purchase-by-supplier");
    }

    private byte[] generateProfitLossReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        BigDecimal revenue = getSalesTotal(startDate, endDate);
        BigDecimal expenses = zeroIfNull(expenseRepository.getTotalExpensesForPeriod(startDate.toLocalDate(), endDate.toLocalDate()));
        BigDecimal grossProfit = revenue.subtract(expenses);

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Profit & Loss Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("revenue", revenue);
        data.put("expenses", expenses);
        data.put("grossProfit", grossProfit);
        data.put("profitMargin", revenue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : grossProfit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        data.put("revenueByMonth", List.of());
        data.put("expensesByCategory", expenseRepository.getExpensesGroupedByCategory(startDate.toLocalDate(), endDate.toLocalDate()));
        return renderReport(data, format, "profit-loss");
    }

    private byte[] generateExpenseSummaryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Expense Summary Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalExpenses", zeroIfNull(expenseRepository.getTotalExpensesForPeriod(startDate.toLocalDate(), endDate.toLocalDate())));
        data.put("expensesByCategory", expenseRepository.getExpensesGroupedByCategory(startDate.toLocalDate(), endDate.toLocalDate()));
        return renderReport(data, format, "expense-summary");
    }

    private byte[] generateExpenseDetailedReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Detailed Expense Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("expenses", List.of());
        return renderReport(data, format, "expense-detailed");
    }

    private byte[] generateExpenseByCategoryReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Expenses by Category Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("expensesByCategory", expenseRepository.getExpensesGroupedByCategory(startDate.toLocalDate(), endDate.toLocalDate()));
        return renderReport(data, format, "expense-by-category");
    }

    private byte[] generateRevenueReport(LocalDateTime startDate, LocalDateTime endDate, ReportFormat format) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Revenue Report");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalRevenue", getSalesTotal(startDate, endDate));
        data.put("revenueByMonth", List.of());
        return renderReport(data, format, "revenue");
    }

    private byte[] generateCustomerDuesReport(ReportFormat format) {
        List<Map<String, Object>> dues = outstandingByCustomer().entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(entry -> Map.<String, Object>of(
                        "customerId", entry.getKey(),
                        "amount", entry.getValue()
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Dues Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerDues", dues);
        return renderReport(data, format, "customer-dues");
    }

    private byte[] generateCustomerPaymentsReport(ReportFormat format) {
        List<Map<String, Object>> payments = customerReceiptRepository.findAll().stream()
                .map(receipt -> Map.<String, Object>of(
                        "receiptNumber", receipt.getReceiptNumber(),
                        "customerId", receipt.getCustomerId(),
                        "receiptDate", receipt.getReceiptDate(),
                        "paymentMethod", receipt.getPaymentMethod(),
                        "amount", zeroIfNull(receipt.getAmount())
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Payments Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerPayments", payments);
        return renderReport(data, format, "customer-payments");
    }

    private byte[] generateCustomerLtvReport(ReportFormat format) {
        List<Map<String, Object>> ltv = salesInvoiceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "customerId", entry.getKey(),
                        "lifetimeValue", entry.getValue()
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("reportTitle", "Customer Lifetime Value Report");
        data.put("generatedDate", LocalDateTime.now());
        data.put("customerLTV", ltv);
        return renderReport(data, format, "customer-ltv");
    }

    private byte[] renderReport(Map<String, Object> data, ReportFormat format, String templateName) {
        return switch (format) {
            case PDF -> pdfGenerator.generateReport(data, templateName);
            case EXCEL -> excelExporter.generateExcel(data, templateName);
            case CSV -> new byte[0];
            case HTML -> new byte[0];
            case JSON -> new byte[0];
            default -> throw new BusinessException("Unsupported report format: " + format);
        };
    }

    private List<SalesInvoice> getSalesInvoices(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        return salesInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getInvoiceDate() != null)
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(start) && !invoice.getInvoiceDate().isAfter(end))
                .toList();
    }

    private List<PurchaseOrder> getPurchaseOrders(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        return purchaseOrderRepository.findAll().stream()
                .filter(order -> order.getPoDate() != null)
                .filter(order -> !order.getPoDate().isBefore(start) && !order.getPoDate().isAfter(end))
                .toList();
    }

    private BigDecimal getSalesTotal(LocalDateTime startDate, LocalDateTime endDate) {
        return getSalesInvoices(startDate, endDate).stream()
                .map(SalesInvoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getAverageOrderValue(LocalDateTime startDate, LocalDateTime endDate) {
        List<SalesInvoice> invoices = getSalesInvoices(startDate, endDate);
        if (invoices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return getSalesTotal(startDate, endDate).divide(BigDecimal.valueOf(invoices.size()), 2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> getSalesByDay(LocalDateTime startDate, LocalDateTime endDate) {
        return getSalesInvoices(startDate, endDate).stream()
                .collect(Collectors.groupingBy(
                        SalesInvoice::getInvoiceDate,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of("date", entry.getKey(), "amount", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> getSalesByPaymentMethod(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        return customerReceiptRepository.findAll().stream()
                .filter(receipt -> receipt.getReceiptDate() != null)
                .filter(receipt -> !receipt.getReceiptDate().isBefore(start) && !receipt.getReceiptDate().isAfter(end))
                .collect(Collectors.groupingBy(
                        receipt -> receipt.getPaymentMethod() == null ? "UNKNOWN" : receipt.getPaymentMethod(),
                        Collectors.mapping(
                                receipt -> zeroIfNull(receipt.getAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("paymentMethod", entry.getKey(), "amount", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> getTopProductsData(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        Map<Long, SalesInvoice> invoicesById = getSalesInvoices(startDate, endDate).stream()
                .collect(Collectors.toMap(SalesInvoice::getId, Function.identity()));
        Map<Long, StoreProduct> productsById = productRepository.findAll().stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));

        return salesInvoiceLineRepository.findAll().stream()
                .filter(line -> invoicesById.containsKey(line.getSalesInvoiceId()))
                .collect(Collectors.groupingBy(SalesInvoiceLine::getProductId))
                .entrySet().stream()
                .map(entry -> {
                    StoreProduct product = productsById.get(entry.getKey());
                    BigDecimal quantity = entry.getValue().stream()
                            .map(SalesInvoiceLine::getBaseQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal revenue = entry.getValue().stream()
                            .map(SalesInvoiceLine::getLineAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", entry.getKey());
                    row.put("productName", product != null ? product.getName() : "Unknown Product");
                    row.put("sku", product != null ? product.getSku() : null);
                    row.put("quantitySold", quantity);
                    row.put("revenue", revenue);
                    return row;
                })
                .sorted((left, right) -> ((BigDecimal) right.get("revenue")).compareTo((BigDecimal) left.get("revenue")))
                .limit(limit)
                .toList();
    }

    private List<Map<String, Object>> getLowStockItems() {
        Map<Long, StoreProduct> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));
        return inventoryBalanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        InventoryBalance::getProductId,
                        Collectors.mapping(
                                balance -> zeroIfNull(balance.getAvailableBaseQuantity()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(LOW_STOCK_THRESHOLD) <= 0)
                .map(entry -> {
                    StoreProduct product = products.get(entry.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", entry.getKey());
                    row.put("productName", product != null ? product.getName() : "Unknown Product");
                    row.put("sku", product != null ? product.getSku() : null);
                    row.put("currentStock", entry.getValue());
                    return row;
                })
                .toList();
    }

    private long countOutOfStock() {
        return inventoryBalanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        InventoryBalance::getProductId,
                        Collectors.mapping(
                                balance -> zeroIfNull(balance.getAvailableBaseQuantity()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .values().stream()
                .filter(quantity -> quantity.compareTo(BigDecimal.ZERO) <= 0)
                .count();
    }

    private BigDecimal calculateTotalInventoryValue() {
        return inventoryBalanceRepository.findAll().stream()
                .map(balance -> zeroIfNull(balance.getAvailableBaseQuantity()).multiply(zeroIfNull(balance.getAvgCost())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> outstandingByCustomer() {
        Map<Long, BigDecimal> charges = salesInvoiceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Long, BigDecimal> payments = customerReceiptRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        CustomerReceipt::getCustomerId,
                        Collectors.mapping(
                                receipt -> zeroIfNull(receipt.getAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return charges.keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        customerId -> charges.getOrDefault(customerId, BigDecimal.ZERO)
                                .subtract(payments.getOrDefault(customerId, BigDecimal.ZERO))
                                .max(BigDecimal.ZERO)
                ));
    }

    private BigDecimal getTotalOutstanding() {
        return sumAmounts(outstandingByCustomer().values());
    }

    private long countOverdueCustomers() {
        Map<Long, BigDecimal> overdueCharges = salesInvoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getInvoiceDate() != null && invoice.getInvoiceDate().isBefore(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Long, BigDecimal> payments = customerReceiptRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        CustomerReceipt::getCustomerId,
                        Collectors.mapping(
                                receipt -> zeroIfNull(receipt.getAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return overdueCharges.keySet().stream()
                .map(customerId -> overdueCharges.getOrDefault(customerId, BigDecimal.ZERO)
                        .subtract(payments.getOrDefault(customerId, BigDecimal.ZERO)))
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private List<Map<String, Object>> getTopCustomers() {
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return outstandingByCustomer().entrySet().stream()
                .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
                .limit(5)
                .map(entry -> {
                    Customer customer = customers.get(entry.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("customerId", entry.getKey());
                    row.put("customerName", customer != null ? customer.getFullName() : "Unknown Customer");
                    row.put("amount", entry.getValue());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> toNamedAmountMaps(List<Object[]> rows, String nameKey) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(nameKey, row.length > 0 ? row[0] : null);
                    map.put("amount", row.length > 1 ? row[1] : BigDecimal.ZERO);
                    return map;
                })
                .toList();
    }

    private BigDecimal sumAmounts(Collection<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isPendingPurchaseOrder(PurchaseOrder order) {
        return order.getStatus() != null && PENDING_PURCHASE_STATUSES.contains(order.getStatus().toUpperCase());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
