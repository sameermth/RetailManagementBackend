package com.retailmanagement.modules.sales.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.sales.dto.request.PaymentRequest;
import com.retailmanagement.modules.sales.dto.response.PaymentResponse;
import com.retailmanagement.modules.sales.enums.PaymentStatus;
import com.retailmanagement.modules.sales.mapper.PaymentMapper;
import com.retailmanagement.modules.sales.model.Payment;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.repository.PaymentRepository;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.sales.service.PaymentService;
import com.retailmanagement.modules.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final SalesService salesService;
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for sale ID: {}", request.getSaleId());

        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + request.getSaleId()));

        // Check if payment amount is valid
        if (request.getAmount().compareTo(sale.getPendingAmount()) > 0) {
            throw new BusinessException("Payment amount exceeds pending amount. Pending: " +
                    sale.getPendingAmount() + ", Requested: " + request.getAmount());
        }

        // Generate payment reference
        String paymentReference = generatePaymentReference();

        // Create payment
        Payment payment = paymentMapper.toEntity(request);
        payment.setPaymentReference(paymentReference);
        payment.setSale(sale);
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceivedBy("SYSTEM"); // In real app, get from SecurityContext

        // Set payment method specific fields
        switch (request.getPaymentMethod()) {
            case CHEQUE:
                if (request.getChequeNumber() == null) {
                    throw new BusinessException("Cheque number is required for cheque payments");
                }
                payment.setStatus(PaymentStatus.PENDING); // Cheque needs clearance
                break;
            case CARD:
                if (request.getTransactionId() == null) {
                    throw new BusinessException("Transaction ID is required for card payments");
                }
                break;
            case UPI:
                if (request.getUpiId() == null) {
                    throw new BusinessException("UPI ID is required for UPI payments");
                }
                break;
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Update sale payment status
        salesService.updatePaymentStatus(sale.getId());

        log.info("Payment created successfully with reference: {}", paymentReference);

        return paymentMapper.toResponse(savedPayment);
    }

    private String generatePaymentReference() {
        String reference;
        do {
            reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paymentRepository.existsByTransactionId(reference));
        return reference;
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        log.debug("Fetching payment with ID: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByReference(String reference) {
        log.debug("Fetching payment with reference: {}", reference);

        Payment payment = paymentRepository.findPaymentByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + reference));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        log.debug("Fetching all payments with pagination");

        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<PaymentResponse> getPaymentsBySale(Long saleId) {
        log.debug("Fetching payments for sale ID: {}", saleId);

        return paymentRepository.findBySaleId(saleId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching payments between {} and {}", startDate, endDate);

        return paymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelPayment(Long id, String reason) {
        log.info("Cancelling payment with ID: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment is already cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setNotes(payment.getNotes() + " [Cancelled: " + reason + "]");
        paymentRepository.save(payment);

        // Update sale payment status
        salesService.updatePaymentStatus(payment.getSale().getId());

        log.info("Payment cancelled successfully with ID: {}", id);
    }

    @Override
    public Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = paymentRepository.getTotalPaymentsForPeriod(startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    public boolean isTransactionIdUnique(String transactionId) {
        return !paymentRepository.existsByTransactionId(transactionId);
    }
}