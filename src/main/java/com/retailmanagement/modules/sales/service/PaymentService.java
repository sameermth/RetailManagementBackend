package com.retailmanagement.modules.sales.service;

import com.retailmanagement.modules.sales.dto.request.PaymentRequest;
import com.retailmanagement.modules.sales.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByReference(String reference);

    Page<PaymentResponse> getAllPayments(Pageable pageable);

    List<PaymentResponse> getPaymentsBySale(Long saleId);

    List<PaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void cancelPayment(Long id, String reason);

    Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate);

    boolean isTransactionIdUnique(String transactionId);
}