package com.retailmanagement.modules.distributor.service;

import com.retailmanagement.modules.distributor.dto.request.DistributorPaymentRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface DistributorPaymentService {

    DistributorPaymentResponse createPayment(DistributorPaymentRequest request);

    DistributorPaymentResponse getPaymentById(Long id);

    DistributorPaymentResponse getPaymentByReference(String reference);

    Page<DistributorPaymentResponse> getAllPayments(Pageable pageable);

    List<DistributorPaymentResponse> getPaymentsByDistributor(Long distributorId);

    Page<DistributorPaymentResponse> getPaymentsByDistributor(Long distributorId, Pageable pageable);

    List<DistributorPaymentResponse> getPaymentsByOrder(Long orderId);

    List<DistributorPaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void cancelPayment(Long id, String reason);

    Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate);

    Double getTotalPaymentsByDistributor(Long distributorId);
}