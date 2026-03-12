package com.retailmanagement.modules.sales.service;

import com.retailmanagement.modules.sales.dto.request.SaleRequest;
import com.retailmanagement.modules.sales.dto.response.SaleResponse;
import com.retailmanagement.modules.sales.dto.response.SaleSummaryResponse;
import com.retailmanagement.modules.sales.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesService {

    SaleResponse createSale(SaleRequest request);

    SaleResponse updateSale(Long id, SaleRequest request);

    SaleResponse getSaleById(Long id);

    SaleResponse getSaleByInvoiceNumber(String invoiceNumber);

    Page<SaleResponse> getAllSales(Pageable pageable);

    List<SaleResponse> getSalesByCustomer(Long customerId);

    Page<SaleResponse> getSalesByCustomer(Long customerId, Pageable pageable);

    List<SaleResponse> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<SaleResponse> getSalesByStatus(SaleStatus status);

    void cancelSale(Long id, String reason);

    SaleResponse processReturn(Long id, String reason, List<Long> itemIds);

    void updatePaymentStatus(Long id);

    Double getTotalSales(LocalDateTime startDate, LocalDateTime endDate);

    Long getSalesCount(LocalDateTime startDate, LocalDateTime endDate);

    List<SaleSummaryResponse> getRecentSales(int limit);

    boolean isInvoiceNumberUnique(String invoiceNumber);
}