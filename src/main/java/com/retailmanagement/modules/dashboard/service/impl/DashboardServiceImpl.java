package com.retailmanagement.modules.dashboard.service.impl;

import com.retailmanagement.modules.dashboard.dto.DashboardSummaryDTO;
import com.retailmanagement.modules.dashboard.dto.DashboardAnalyticsDTOs;
import com.retailmanagement.modules.dashboard.dto.DueSummaryDTO;
import com.retailmanagement.modules.dashboard.dto.LowStockAlertDTO;
import com.retailmanagement.modules.dashboard.dto.RecentActivityDTO;
import com.retailmanagement.modules.dashboard.dto.SalesSummaryDTO;
import com.retailmanagement.modules.dashboard.dto.TopProductDTO;
import com.retailmanagement.modules.dashboard.service.DashboardService;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.finance.dto.ErpFinanceDtos;
import com.retailmanagement.modules.erp.finance.service.ErpFinanceService;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.tax.service.TaxRegistrationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal LOW_STOCK_THRESHOLD = BigDecimal.valueOf(5);
    private static final BigDecimal REORDER_TARGET = BigDecimal.TEN;
    private static final Set<String> COMPLETED_INVOICE_STATUSES = Set.of("POSTED", "COMPLETED", "PAID");
    private static final Set<String> PENDING_ORDER_STATUSES = Set.of("DRAFT", "OPEN", "PENDING", "SUBMITTED");

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final StoreProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final TaxRegistrationService taxRegistrationService;
    private final ErpFinanceService erpFinanceService;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        log.info("Fetching dashboard summary from ERP data");
        Long organizationId = currentOrganizationId();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        BigDecimal totalDueAmount = getTotalOutstandingAmount(organizationId);

        return DashboardSummaryDTO.builder()
                .todaySales(getTodaySales())
                .weeklySales(getSalesForPeriod(weekStart, today))
                .monthlySales(getSalesForPeriod(monthStart, today))
                .totalProducts(productRepository.findByOrganizationId(organizationId).size())
                .lowStockCount(getLowStockAlerts().size())
                .outOfStockCount((int) availableQuantityByProduct(organizationId).values().stream()
                        .filter(quantity -> quantity.compareTo(BigDecimal.ZERO) <= 0)
                        .count())
                .totalCustomers(customerRepository.findByOrganizationId(organizationId).size())
                .newCustomersToday((int) customerRepository.findByOrganizationId(organizationId).stream()
                        .map(Customer::getCreatedAt)
                        .filter(Objects::nonNull)
                        .filter(createdAt -> createdAt.toLocalDate().isEqual(today))
                        .count())
                .totalDueAmount(totalDueAmount)
                .overdueCount((int) overdueOutstandingByCustomer(organizationId).values().stream()
                        .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                        .count())
                .pendingOrders((int) purchaseOrderRepository.findByOrganizationIdOrderByPoDateDescIdDesc(organizationId).stream()
                        .filter(this::isPendingPurchaseOrder)
                        .count())
                .completedOrdersToday((int) salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                        .filter(invoice -> today.equals(invoice.getInvoiceDate()))
                        .filter(this::isCompletedInvoice)
                        .count())
                .gstStatus(taxRegistrationService.thresholdStatus(organizationId, today))
                .build();
    }

    @Override
    public SalesSummaryDTO getTodaySales() {
        LocalDate today = LocalDate.now();
        return getSalesForPeriod(today, today);
    }

    @Override
    public SalesSummaryDTO getSalesForPeriod(LocalDate startDate, LocalDate endDate) {
        Long organizationId = currentOrganizationId();
        List<SalesInvoice> invoices = salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .filter(invoice -> isWithinRange(invoice.getInvoiceDate(), startDate, endDate))
                .toList();
        List<CustomerReceipt> receipts = customerReceiptRepository.findByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId).stream()
                .filter(receipt -> isWithinRange(receipt.getReceiptDate(), startDate, endDate))
                .toList();

        BigDecimal totalAmount = sumInvoiceTotals(invoices);
        int totalTransactions = invoices.size();

        return SalesSummaryDTO.builder()
                .totalAmount(totalAmount)
                .totalTransactions(totalTransactions)
                .averageTransactionValue(totalTransactions == 0
                        ? BigDecimal.ZERO
                        : totalAmount.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP))
                .cashAmount(sumReceiptsByMethod(receipts, "CASH"))
                .cardAmount(sumReceiptsByMethod(receipts, "CARD"))
                .upiAmount(sumReceiptsByMethod(receipts, "UPI"))
                .creditAmount(sumReceiptsByMethod(receipts, "CREDIT"))
                .build();
    }

    @Override
    public List<TopProductDTO> getTopProducts(int limit) {
        Long organizationId = currentOrganizationId();
        LocalDate startDate = LocalDate.now().minusDays(30);
        Map<Long, SalesInvoice> invoicesById = salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .filter(invoice -> invoice.getInvoiceDate() != null && !invoice.getInvoiceDate().isBefore(startDate))
                .collect(Collectors.toMap(SalesInvoice::getId, Function.identity()));
        Map<Long, StoreProduct> productsById = productRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));

        return salesInvoiceLineRepository.findAll().stream()
                .filter(line -> invoicesById.containsKey(line.getSalesInvoiceId()))
                .collect(Collectors.groupingBy(SalesInvoiceLine::getProductId))
                .entrySet().stream()
                .map(entry -> toTopProduct(entry.getKey(), entry.getValue(), productsById.get(entry.getKey())))
                .sorted(Comparator.comparing(TopProductDTO::getTotalRevenue, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<LowStockAlertDTO> getLowStockAlerts() {
        Long organizationId = currentOrganizationId();
        Map<Long, BigDecimal> quantitiesByProduct = availableQuantityByProduct(organizationId);

        return productRepository.findByOrganizationId(organizationId).stream()
                .map(product -> toLowStockAlert(product, quantitiesByProduct.getOrDefault(product.getId(), BigDecimal.ZERO)))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(LowStockAlertDTO::getCurrentStock))
                .toList();
    }

    @Override
    public List<RecentActivityDTO> getRecentActivities(int limit) {
        Long organizationId = currentOrganizationId();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        List<RecentActivityDTO> activities = new ArrayList<>();
        salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .filter(invoice -> invoice.getCreatedAt() != null && !invoice.getCreatedAt().isBefore(cutoff))
                .map(this::toSaleActivity)
                .forEach(activities::add);
        purchaseOrderRepository.findByOrganizationIdOrderByPoDateDescIdDesc(organizationId).stream()
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(cutoff))
                .map(this::toPurchaseActivity)
                .forEach(activities::add);
        customerReceiptRepository.findByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId).stream()
                .filter(receipt -> receipt.getCreatedAt() != null && !receipt.getCreatedAt().isBefore(cutoff))
                .map(this::toReceiptActivity)
                .forEach(activities::add);
        customerRepository.findByOrganizationId(organizationId).stream()
                .filter(customer -> customer.getCreatedAt() != null && !customer.getCreatedAt().isBefore(cutoff))
                .map(this::toCustomerActivity)
                .forEach(activities::add);

        return activities.stream()
                .sorted(Comparator.comparing(RecentActivityDTO::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public DueSummaryDTO getDueSummary() {
        Long organizationId = currentOrganizationId();
        Map<Long, BigDecimal> outstandingByCustomer = outstandingByCustomer(organizationId);
        Map<Long, BigDecimal> overdueByCustomer = overdueOutstandingByCustomer(organizationId);

        return DueSummaryDTO.builder()
                .totalDueAmount(sumAmounts(outstandingByCustomer.values()))
                .totalDueCustomers((int) outstandingByCustomer.values().stream()
                        .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                        .count())
                .overdueAmount(sumAmounts(overdueByCustomer.values()))
                .overdueCount((int) overdueByCustomer.values().stream()
                        .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                        .count())
                .dueThisWeek(BigDecimal.ZERO)
                .dueNextWeek(BigDecimal.ZERO)
                .upcomingDues(List.of())
                .build();
    }

    @Override
    public List<DueSummaryDTO.UpcomingDueDTO> getUpcomingDues(int days) {
        Long organizationId = currentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        Map<Long, Customer> customers = customerRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return salesInvoiceRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId).stream()
                .filter(invoice -> invoice.getDueDate() != null)
                .filter(invoice -> !invoice.getDueDate().isBefore(today) && !invoice.getDueDate().isAfter(end))
                .map(invoice -> {
                    BigDecimal outstanding = outstandingAmount(invoice);
                    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    Customer customer = customers.get(invoice.getCustomerId());
                    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, invoice.getDueDate());
                    return DueSummaryDTO.UpcomingDueDTO.builder()
                            .customerId(invoice.getCustomerId())
                            .customerName(customer == null ? "Customer " + invoice.getCustomerId() : customer.getFullName())
                            .customerPhone(customer == null ? null : customer.getPhone())
                            .dueAmount(outstanding)
                            .dueDate(invoice.getDueDate())
                            .daysRemaining((int) daysRemaining)
                            .status(daysRemaining == 0 ? "TODAY" : "UPCOMING")
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public DashboardAnalyticsDTOs.ProfitabilitySummaryDTO getProfitabilitySummary(LocalDate startDate, LocalDate endDate, int limit) {
        Long organizationId = currentOrganizationId();
        List<SalesInvoice> invoices = salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .filter(this::isCompletedInvoice)
                .filter(invoice -> isWithinRange(invoice.getInvoiceDate(), startDate, endDate))
                .toList();
        Map<Long, StoreProduct> productsById = productRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));
        List<SalesInvoiceLine> lines = invoices.stream()
                .flatMap(invoice -> salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId()).stream())
                .toList();

        BigDecimal revenue = lines.stream().map(SalesInvoiceLine::getTaxableAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = lines.stream().map(SalesInvoiceLine::getTotalCostAtSale).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = revenue.subtract(cost);

        List<DashboardAnalyticsDTOs.ProfitabilityProductDTO> topProducts = lines.stream()
                .collect(Collectors.groupingBy(SalesInvoiceLine::getProductId))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal productRevenue = entry.getValue().stream().map(SalesInvoiceLine::getTaxableAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal productCost = entry.getValue().stream().map(SalesInvoiceLine::getTotalCostAtSale).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal productGrossProfit = productRevenue.subtract(productCost);
                    StoreProduct product = productsById.get(entry.getKey());
                    return new DashboardAnalyticsDTOs.ProfitabilityProductDTO(
                            entry.getKey(),
                            product == null ? "Unknown Product" : product.getName(),
                            product == null ? null : product.getSku(),
                            productRevenue,
                            productCost,
                            productGrossProfit,
                            percent(productGrossProfit, productRevenue)
                    );
                })
                .sorted(Comparator.comparing(DashboardAnalyticsDTOs.ProfitabilityProductDTO::grossProfit).reversed())
                .limit(limit)
                .toList();

        return new DashboardAnalyticsDTOs.ProfitabilitySummaryDTO(
                startDate,
                endDate,
                revenue,
                cost,
                grossProfit,
                percent(grossProfit, revenue),
                invoices.size(),
                topProducts
        );
    }

    @Override
    public DashboardAnalyticsDTOs.AgingDashboardDTO getAgingDashboard(LocalDate asOfDate) {
        Long organizationId = currentOrganizationId();
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        ErpFinanceService.OutstandingSummary customerSummary = erpFinanceService.outstanding(
                organizationId,
                new ErpFinanceDtos.OutstandingQuery("CUSTOMER", null, effectiveDate)
        );
        ErpFinanceService.OutstandingSummary supplierSummary = erpFinanceService.outstanding(
                organizationId,
                new ErpFinanceDtos.OutstandingQuery("SUPPLIER", null, effectiveDate)
        );
        return new DashboardAnalyticsDTOs.AgingDashboardDTO(
                effectiveDate,
                toAgingSummary(customerSummary),
                toAgingSummary(supplierSummary)
        );
    }

    @Override
    public DashboardAnalyticsDTOs.StockSummaryDTO getStockSummary(int limit) {
        Long organizationId = currentOrganizationId();
        Map<Long, StoreProduct> productsById = productRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity()));
        Map<Long, List<InventoryBalance>> balancesByProduct = inventoryBalanceRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.groupingBy(InventoryBalance::getProductId));

        BigDecimal onHand = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        List<DashboardAnalyticsDTOs.StockProductSnapshotDTO> lowStockProducts = new ArrayList<>();

        for (StoreProduct product : productsById.values()) {
            List<InventoryBalance> balances = balancesByProduct.getOrDefault(product.getId(), List.of());
            BigDecimal productOnHand = balances.stream().map(InventoryBalance::getOnHandBaseQuantity).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal productReserved = balances.stream().map(InventoryBalance::getReservedBaseQuantity).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal productAvailable = balances.stream().map(InventoryBalance::getAvailableBaseQuantity).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal productValue = balances.stream()
                    .map(balance -> zeroIfNull(balance.getOnHandBaseQuantity()).multiply(zeroIfNull(balance.getAvgCost())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            onHand = onHand.add(productOnHand);
            reserved = reserved.add(productReserved);
            available = available.add(productAvailable);
            inventoryValue = inventoryValue.add(productValue);

            BigDecimal reorderLevel = zeroIfNull(product.getReorderLevelBaseQty());
            if (productAvailable.compareTo(reorderLevel) <= 0) {
                lowStockProducts.add(new DashboardAnalyticsDTOs.StockProductSnapshotDTO(
                        product.getId(),
                        product.getName(),
                        product.getSku(),
                        productOnHand,
                        productReserved,
                        productAvailable,
                        productValue,
                        productAvailable.compareTo(BigDecimal.ZERO) <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK"
                ));
            }
        }

        lowStockProducts.sort(Comparator.comparing(DashboardAnalyticsDTOs.StockProductSnapshotDTO::availableQuantity));

        return new DashboardAnalyticsDTOs.StockSummaryDTO(
                onHand,
                reserved,
                available,
                inventoryValue,
                lowStockProducts.size(),
                (int) lowStockProducts.stream().filter(item -> item.availableQuantity().compareTo(BigDecimal.ZERO) <= 0).count(),
                lowStockProducts.stream().limit(limit).toList()
        );
    }

    @Override
    public DashboardAnalyticsDTOs.TaxSummaryDTO getTaxSummary(LocalDate startDate, LocalDate endDate) {
        Long organizationId = currentOrganizationId();
        BigDecimal outputTax = BigDecimal.ZERO;
        BigDecimal inputTax = BigDecimal.ZERO;
        BigDecimal salesReturnTax = BigDecimal.ZERO;
        BigDecimal purchaseReturnTax = BigDecimal.ZERO;
        BigDecimal taxableSales = BigDecimal.ZERO;
        BigDecimal taxablePurchases = BigDecimal.ZERO;

        for (SalesInvoice invoice : salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId)) {
            if (!isCompletedInvoice(invoice) || !isWithinRange(invoice.getInvoiceDate(), startDate, endDate)) {
                continue;
            }
            outputTax = outputTax.add(zeroIfNull(invoice.getTaxAmount()));
            taxableSales = taxableSales.add(zeroIfNull(invoice.getSubtotal()).subtract(zeroIfNull(invoice.getDiscountAmount())));
        }
        for (PurchaseReceipt receipt : purchaseReceiptRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId)) {
            if (!isWithinRange(receipt.getReceiptDate(), startDate, endDate)) {
                continue;
            }
            inputTax = inputTax.add(zeroIfNull(receipt.getTaxAmount()));
            taxablePurchases = taxablePurchases.add(zeroIfNull(receipt.getSubtotal()));
        }
        for (SalesReturn salesReturn : salesReturnRepository.findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(organizationId, startDate, endDate)) {
            if (!"POSTED".equalsIgnoreCase(salesReturn.getStatus())) {
                continue;
            }
            salesReturnTax = salesReturnTax.add(zeroIfNull(salesReturn.getTaxAmount()));
        }
        for (PurchaseReturn purchaseReturn : purchaseReturnRepository.findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(organizationId, startDate, endDate)) {
            if (!"POSTED".equalsIgnoreCase(purchaseReturn.getStatus())) {
                continue;
            }
            purchaseReturnTax = purchaseReturnTax.add(zeroIfNull(purchaseReturn.getTaxAmount()));
        }

        var gstStatus = taxRegistrationService.thresholdStatus(organizationId, endDate);
        return new DashboardAnalyticsDTOs.TaxSummaryDTO(
                startDate,
                endDate,
                outputTax,
                inputTax,
                salesReturnTax,
                purchaseReturnTax,
                outputTax.subtract(salesReturnTax).subtract(inputTax.subtract(purchaseReturnTax)),
                taxableSales,
                taxablePurchases,
                gstStatus.alertLevel(),
                gstStatus.message()
        );
    }

    private TopProductDTO toTopProduct(Long productId, List<SalesInvoiceLine> lines, StoreProduct product) {
        BigDecimal quantitySold = lines.stream()
                .map(SalesInvoiceLine::getBaseQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = lines.stream()
                .map(SalesInvoiceLine::getLineAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TopProductDTO.builder()
                .productId(productId)
                .productName(product != null ? product.getName() : "Unknown Product")
                .sku(product != null ? product.getSku() : null)
                .category(null)
                .quantitySold(quantitySold.intValue())
                .totalRevenue(totalRevenue)
                .averagePrice(quantitySold.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : totalRevenue.divide(quantitySold, 2, RoundingMode.HALF_UP))
                .build();
    }

    private LowStockAlertDTO toLowStockAlert(StoreProduct product, BigDecimal quantity) {
        if (quantity.compareTo(LOW_STOCK_THRESHOLD) > 0) {
            return null;
        }

        int currentStock = quantity.max(BigDecimal.ZERO).intValue();
        int reorderLevel = LOW_STOCK_THRESHOLD.intValue();
        int recommendedOrder = REORDER_TARGET.subtract(quantity.max(BigDecimal.ZERO)).max(BigDecimal.ZERO).intValue();

        return LowStockAlertDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .category(null)
                .currentStock(currentStock)
                .reorderLevel(reorderLevel)
                .recommendedOrder(recommendedOrder)
                .status(quantity.compareTo(BigDecimal.ZERO) <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK")
                .build();
    }

    private RecentActivityDTO toSaleActivity(SalesInvoice invoice) {
        return RecentActivityDTO.builder()
                .id(invoice.getId())
                .type("SALE")
                .description("Sales invoice " + invoice.getInvoiceNumber() + " created")
                .reference(invoice.getInvoiceNumber())
                .timestamp(invoice.getCreatedAt())
                .status(invoice.getStatus())
                .amount(zeroIfNull(invoice.getTotalAmount()))
                .build();
    }

    private RecentActivityDTO toPurchaseActivity(PurchaseOrder order) {
        return RecentActivityDTO.builder()
                .id(order.getId())
                .type("PURCHASE")
                .description("Purchase order " + order.getPoNumber() + " created")
                .reference(order.getPoNumber())
                .timestamp(order.getCreatedAt())
                .status(order.getStatus())
                .amount(zeroIfNull(order.getTotalAmount()))
                .build();
    }

    private RecentActivityDTO toReceiptActivity(CustomerReceipt receipt) {
        return RecentActivityDTO.builder()
                .id(receipt.getId())
                .type("PAYMENT")
                .description("Customer receipt " + receipt.getReceiptNumber() + " recorded")
                .reference(receipt.getReceiptNumber())
                .timestamp(receipt.getCreatedAt())
                .status(receipt.getStatus())
                .amount(zeroIfNull(receipt.getAmount()))
                .build();
    }

    private RecentActivityDTO toCustomerActivity(Customer customer) {
        return RecentActivityDTO.builder()
                .id(customer.getId())
                .type("CUSTOMER")
                .description("Customer " + customer.getFullName() + " added")
                .reference(customer.getCustomerCode())
                .timestamp(customer.getCreatedAt())
                .status(customer.getStatus())
                .amount(BigDecimal.ZERO)
                .build();
    }

    private Map<Long, BigDecimal> availableQuantityByProduct(Long organizationId) {
        return inventoryBalanceRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.groupingBy(
                        InventoryBalance::getProductId,
                        Collectors.mapping(
                                balance -> zeroIfNull(balance.getAvailableBaseQuantity()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    private Map<Long, BigDecimal> outstandingByCustomer(Long organizationId) {
        Map<Long, BigDecimal> salesTotals = salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Long, BigDecimal> receiptTotals = customerReceiptRepository.findByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId).stream()
                .collect(Collectors.groupingBy(
                        CustomerReceipt::getCustomerId,
                        Collectors.mapping(
                                receipt -> zeroIfNull(receipt.getAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return mergeOutstanding(salesTotals, receiptTotals);
    }

    private Map<Long, BigDecimal> overdueOutstandingByCustomer(Long organizationId) {
        Map<Long, BigDecimal> overdueInvoiceTotals = salesInvoiceRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId).stream()
                .filter(invoice -> invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        SalesInvoice::getCustomerId,
                        Collectors.mapping(
                                invoice -> zeroIfNull(invoice.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Long, BigDecimal> receiptTotals = customerReceiptRepository.findByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId).stream()
                .collect(Collectors.groupingBy(
                        CustomerReceipt::getCustomerId,
                        Collectors.mapping(
                                receipt -> zeroIfNull(receipt.getAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return mergeOutstanding(overdueInvoiceTotals, receiptTotals);
    }

    private Map<Long, BigDecimal> mergeOutstanding(Map<Long, BigDecimal> chargeTotals, Map<Long, BigDecimal> paymentTotals) {
        return chargeTotals.keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        customerId -> chargeTotals.getOrDefault(customerId, BigDecimal.ZERO)
                                .subtract(paymentTotals.getOrDefault(customerId, BigDecimal.ZERO))
                                .max(BigDecimal.ZERO)
                ));
    }

    private BigDecimal getTotalOutstandingAmount(Long organizationId) {
        return sumAmounts(outstandingByCustomer(organizationId).values());
    }

    private BigDecimal sumInvoiceTotals(List<SalesInvoice> invoices) {
        return invoices.stream()
                .map(SalesInvoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumReceiptsByMethod(List<CustomerReceipt> receipts, String paymentMethod) {
        return receipts.stream()
                .filter(receipt -> paymentMethod.equalsIgnoreCase(receipt.getPaymentMethod()))
                .map(CustomerReceipt::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmounts(Collection<BigDecimal> amounts) {
        return amounts.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isWithinRange(LocalDate value, LocalDate startDate, LocalDate endDate) {
        return value != null && !value.isBefore(startDate) && !value.isAfter(endDate);
    }

    private boolean isCompletedInvoice(SalesInvoice invoice) {
        return invoice.getStatus() != null && COMPLETED_INVOICE_STATUSES.contains(invoice.getStatus().toUpperCase());
    }

    private boolean isPendingPurchaseOrder(PurchaseOrder order) {
        return order.getStatus() != null && PENDING_ORDER_STATUSES.contains(order.getStatus().toUpperCase());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long currentOrganizationId() {
        return ErpSecurityUtils.currentOrganizationId().orElse(1L);
    }

    private BigDecimal outstandingAmount(SalesInvoice invoice) {
        BigDecimal allocated = customerReceiptRepository.findByOrganizationIdOrderByReceiptDateDescIdDesc(invoice.getOrganizationId()).stream()
                .filter(receipt -> Objects.equals(receipt.getCustomerId(), invoice.getCustomerId()))
                .map(CustomerReceipt::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return zeroIfNull(invoice.getTotalAmount()).subtract(allocated).max(BigDecimal.ZERO);
    }

    private DashboardAnalyticsDTOs.AgingSummaryDTO toAgingSummary(ErpFinanceService.OutstandingSummary summary) {
        return new DashboardAnalyticsDTOs.AgingSummaryDTO(
                summary.totalOutstanding(),
                summary.aging().current(),
                summary.aging().bucket1To30(),
                summary.aging().bucket31To60(),
                summary.aging().bucket61To90(),
                summary.aging().bucket90Plus()
        );
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
