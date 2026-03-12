package com.retailmanagement.modules.purchase.service;

import com.retailmanagement.modules.purchase.dto.request.SupplierPaymentRequest;
import com.retailmanagement.modules.purchase.dto.response.SupplierPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentService {

    SupplierPaymentResponse createPayment(SupplierPaymentRequest request);

    SupplierPaymentResponse getPaymentById(Long id);

    SupplierPaymentResponse getPaymentByReference(String reference);

    Page<SupplierPaymentResponse> getAllPayments(Pageable pageable);

    List<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId);

    Page<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId, Pageable pageable);

    List<SupplierPaymentResponse> getPaymentsByPurchase(Long purchaseId);

    List<SupplierPaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void cancelPayment(Long id, String reason);

    Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate);
}