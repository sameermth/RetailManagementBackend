package com.retailmanagement.modules.purchase.service;

import com.retailmanagement.modules.purchase.dto.request.PurchaseReceiptRequest;
import com.retailmanagement.modules.purchase.dto.request.PurchaseRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseReceiptResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseSummaryResponse;
import com.retailmanagement.modules.purchase.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseService {

    PurchaseResponse createPurchase(PurchaseRequest request);

    PurchaseResponse updatePurchase(Long id, PurchaseRequest request);

    PurchaseResponse getPurchaseById(Long id);

    PurchaseResponse getPurchaseByOrderNumber(String orderNumber);

    Page<PurchaseResponse> getAllPurchases(Pageable pageable);

    List<PurchaseResponse> getPurchasesBySupplier(Long supplierId);

    Page<PurchaseResponse> getPurchasesBySupplier(Long supplierId, Pageable pageable);

    List<PurchaseResponse> getPurchasesByStatus(PurchaseStatus status);

    Page<PurchaseResponse> getPurchasesByStatus(PurchaseStatus status, Pageable pageable);

    List<PurchaseResponse> getPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void approvePurchase(Long id);

    void cancelPurchase(Long id, String reason);

    PurchaseReceiptResponse receivePurchase(PurchaseReceiptRequest request);

    void updatePaymentStatus(Long id, Double paidAmount);

    Double getTotalPurchaseAmount(LocalDateTime startDate, LocalDateTime endDate);

    Long getPendingApprovalCount();

    List<PurchaseSummaryResponse> getRecentPurchases(int limit);

    boolean isPurchaseOrderNumberUnique(String orderNumber);
}