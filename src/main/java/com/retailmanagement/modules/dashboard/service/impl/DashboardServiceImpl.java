package com.retailmanagement.modules.dashboard.service.impl;

import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.customer.repository.CustomerDueRepository;
import com.retailmanagement.modules.dashboard.dto.*;
import com.retailmanagement.modules.dashboard.service.DashboardService;
import com.retailmanagement.modules.inventory.repository.InventoryRepository;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.sales.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerDueRepository dueRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        log.info("Fetching dashboard summary");

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        return DashboardSummaryDTO.builder()
                .todaySales(getTodaySales())
                .weeklySales(getSalesForPeriod(weekStart, today))
                .monthlySales(getSalesForPeriod(monthStart, today))
                .totalProducts(productRepository.count())
                .lowStockCount(inventoryRepository.countLowStock())
                .outOfStockCount(inventoryRepository.countOutOfStock())
                .totalCustomers(customerRepository.count())
                .newCustomersToday(customerRepository.countByCreatedDate(today))
                .totalDueAmount(dueRepository.getTotalDueAmount())
                .overdueCount(dueRepository.countOverdue())
                .pendingOrders(saleRepository.countPendingOrders())
                .completedOrdersToday(saleRepository.countCompletedOrders(today))
                .build();
    }

    @Override
    public SalesSummaryDTO getTodaySales() {
        LocalDate today = LocalDate.now();
        return getSalesForPeriod(today, today);
    }

    @Override
    public SalesSummaryDTO getSalesForPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return SalesSummaryDTO.builder()
                .totalAmount(saleRepository.getTotalSalesForPeriod(start, end))
                .totalTransactions(saleRepository.countSalesForPeriod(start, end))
                .averageTransactionValue(saleRepository.getAverageTransactionValue(start, end))
                .cashAmount(paymentRepository.getTotalByMethodForPeriod("CASH", start, end))
                .cardAmount(paymentRepository.getTotalByMethodForPeriod("CARD", start, end))
                .upiAmount(paymentRepository.getTotalByMethodForPeriod("UPI", start, end))
                .creditAmount(paymentRepository.getTotalByMethodForPeriod("CREDIT", start, end))
                .build();
    }

    @Override
    public List<TopProductDTO> getTopProducts(int limit) {
        LocalDateTime monthStart = LocalDate.now().minusDays(30).atStartOfDay();
        return saleRepository.getTopProducts(monthStart, LocalDateTime.now(), limit);
    }

    @Override
    public List<LowStockAlertDTO> getLowStockAlerts() {
        return inventoryRepository.getLowStockAlerts();
    }

    @Override
    public List<RecentActivityDTO> getRecentActivities(int limit) {
        return saleRepository.getRecentActivities(LocalDateTime.now().minusDays(7), limit);
    }

    @Override
    public DueSummaryDTO getDueSummary() {
        return DueSummaryDTO.builder()
                .totalDueAmount(dueRepository.getTotalDueAmount())
                .totalDueCustomers(dueRepository.countCustomersWithDue())
                .overdueAmount(dueRepository.getTotalOverdueAmount())
                .overdueCount(dueRepository.countOverdue())
                .dueThisWeek(dueRepository.getTotalDueForPeriod(LocalDate.now(), LocalDate.now().plusDays(7)))
                .dueNextWeek(dueRepository.getTotalDueForPeriod(LocalDate.now().plusDays(8), LocalDate.now().plusDays(14)))
                .upcomingDues(getUpcomingDues(7))
                .build();
    }

    @Override
    public List<DueSummaryDTO.UpcomingDueDTO> getUpcomingDues(int days) {
        return dueRepository.getUpcomingDues(LocalDate.now(), LocalDate.now().plusDays(days));
    }
}