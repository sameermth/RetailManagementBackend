package com.retailmanagement.modules.distributor.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.distributor.dto.request.DistributorPaymentRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorPaymentResponse;
import com.retailmanagement.modules.distributor.enums.PaymentStatus;
import com.retailmanagement.modules.distributor.mapper.DistributorPaymentMapper;
import com.retailmanagement.modules.distributor.model.Distributor;
import com.retailmanagement.modules.distributor.model.DistributorOrder;
import com.retailmanagement.modules.distributor.model.DistributorPayment;
import com.retailmanagement.modules.distributor.repository.DistributorOrderRepository;
import com.retailmanagement.modules.distributor.repository.DistributorPaymentRepository;
import com.retailmanagement.modules.distributor.repository.DistributorRepository;
import com.retailmanagement.modules.distributor.service.DistributorOrderService;
import com.retailmanagement.modules.distributor.service.DistributorPaymentService;
import com.retailmanagement.modules.distributor.service.DistributorService;
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
public class DistributorPaymentServiceImpl implements DistributorPaymentService {

    private final DistributorPaymentRepository paymentRepository;
    private final DistributorRepository distributorRepository;
    private final DistributorOrderRepository orderRepository;
    private final DistributorOrderService orderService;
    private final DistributorService distributorService;
    private final DistributorPaymentMapper paymentMapper;

    @Override
    public DistributorPaymentResponse createPayment(DistributorPaymentRequest request) {
        log.info("Creating payment for distributor ID: {}", request.getDistributorId());

        Distributor distributor = distributorRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.getDistributorId()));

        DistributorOrder order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

            // Verify order belongs to distributor
            if (!order.getDistributor().getId().equals(distributor.getId())) {
                throw new BusinessException("Order does not belong to this distributor");
            }
        }

        // Generate payment reference
        String paymentReference = generatePaymentReference();

        DistributorPayment payment = paymentMapper.toEntity(request);
        payment.setPaymentReference(paymentReference);
        payment.setDistributor(distributor);
        payment.setOrder(order);
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceivedBy("SYSTEM");

        // Set payment method specific fields
        switch (request.getPaymentMethod()) {
            case CHEQUE:
                if (request.getChequeNumber() == null) {
                    throw new BusinessException("Cheque number is required for cheque payments");
                }
                payment.setStatus(PaymentStatus.PENDING); // Cheque needs clearance
                break;
            case BANK_TRANSFER:
                if (request.getTransactionId() == null) {
                    throw new BusinessException("Transaction ID is required for bank transfers");
                }
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

        DistributorPayment savedPayment = paymentRepository.save(payment);

        // Update distributor's outstanding amount (negative for payments)
        distributorService.updateOutstandingAmount(distributor.getId(), request.getAmount().negate());

        // Update order payment status if payment is for a specific order
        if (order != null && payment.getStatus() == PaymentStatus.PAID) {
            orderService.updatePaymentStatus(order.getId(), request.getAmount().doubleValue());
        }

        log.info("Payment created successfully with reference: {}", paymentReference);

        return paymentMapper.toResponse(savedPayment);
    }

    private String generatePaymentReference() {
        String reference;
        do {
            reference = "DIST-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paymentRepository.existsByPaymentReference(reference));
        return reference;
    }

    @Override
    public DistributorPaymentResponse getPaymentById(Long id) {
        log.debug("Fetching payment with ID: {}", id);

        DistributorPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public DistributorPaymentResponse getPaymentByReference(String reference) {
        log.debug("Fetching payment with reference: {}", reference);

        DistributorPayment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + reference));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public Page<DistributorPaymentResponse> getAllPayments(Pageable pageable) {
        log.debug("Fetching all payments with pagination");

        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<DistributorPaymentResponse> getPaymentsByDistributor(Long distributorId) {
        log.debug("Fetching payments for distributor ID: {}", distributorId);

        return paymentRepository.findByDistributorId(distributorId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DistributorPaymentResponse> getPaymentsByDistributor(Long distributorId, Pageable pageable) {
        log.debug("Fetching payments for distributor ID: {} with pagination", distributorId);

        return paymentRepository.findByDistributorId(distributorId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<DistributorPaymentResponse> getPaymentsByOrder(Long orderId) {
        log.debug("Fetching payments for order ID: {}", orderId);

        return paymentRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistributorPaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching payments between {} and {}", startDate, endDate);

        return paymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelPayment(Long id, String reason) {
        log.info("Cancelling payment with ID: {}", id);

        DistributorPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment is already cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setNotes(payment.getNotes() + " [CANCELLED: " + reason + "]");
        paymentRepository.save(payment);

        // Reverse the outstanding amount update
        distributorService.updateOutstandingAmount(payment.getDistributor().getId(), payment.getAmount());

        // Update order payment status if payment was for a specific order
        if (payment.getOrder() != null) {
            orderService.updatePaymentStatus(payment.getOrder().getId(), -payment.getAmount().doubleValue());
        }

        log.info("Payment cancelled successfully with ID: {}", id);
    }

    @Override
    public Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = paymentRepository.getTotalPaymentsForPeriod(startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    public Double getTotalPaymentsByDistributor(Long distributorId) {
        BigDecimal total = paymentRepository.getTotalPaymentsByDistributor(distributorId);
        return total != null ? total.doubleValue() : 0.0;
    }
}