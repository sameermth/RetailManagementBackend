package com.retailmanagement.modules.purchase.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.purchase.dto.request.SupplierPaymentRequest;
import com.retailmanagement.modules.purchase.dto.response.SupplierPaymentResponse;
import com.retailmanagement.modules.purchase.enums.PaymentStatus;
import com.retailmanagement.modules.purchase.mapper.SupplierPaymentMapper;
import com.retailmanagement.modules.purchase.model.Purchase;
import com.retailmanagement.modules.purchase.model.SupplierPayment;
import com.retailmanagement.modules.purchase.repository.PurchaseRepository;
import com.retailmanagement.modules.purchase.repository.SupplierPaymentRepository;
import com.retailmanagement.modules.purchase.service.SupplierPaymentService;
import com.retailmanagement.modules.purchase.service.PurchaseService;
import com.retailmanagement.modules.supplier.model.Supplier;
import com.retailmanagement.modules.supplier.repository.SupplierRepository;
import com.retailmanagement.modules.supplier.service.SupplierService;
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
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final SupplierPaymentMapper paymentMapper;

    @Override
    public SupplierPaymentResponse createPayment(SupplierPaymentRequest request) {
        log.info("Creating payment for supplier ID: {}", request.getSupplierId());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = null;
        if (request.getPurchaseId() != null) {
            purchase = purchaseRepository.findById(request.getPurchaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + request.getPurchaseId()));

            // Verify purchase belongs to supplier
            if (!purchase.getSupplier().getId().equals(supplier.getId())) {
                throw new BusinessException("Purchase does not belong to this supplier");
            }
        }

        // Generate payment reference
        String paymentReference = generatePaymentReference();

        SupplierPayment payment = paymentMapper.toEntity(request);
        payment.setPaymentReference(paymentReference);
        payment.setSupplier(supplier);
        payment.setPurchase(purchase);
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

        SupplierPayment savedPayment = paymentRepository.save(payment);

        // Update supplier's outstanding amount (negative for payments)
        supplierService.updateOutstandingAmount(supplier.getId(), request.getAmount().negate());

        // Update purchase payment status if payment is for a specific purchase
        if (purchase != null && payment.getStatus() == PaymentStatus.PAID) {
            purchaseService.updatePaymentStatus(purchase.getId(), request.getAmount().doubleValue());
        }

        log.info("Payment created successfully with reference: {}", paymentReference);

        return paymentMapper.toResponse(savedPayment);
    }

    private String generatePaymentReference() {
        String reference;
        do {
            reference = "SUP-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paymentRepository.existsByPaymentReference(reference));
        return reference;
    }

    @Override
    public SupplierPaymentResponse getPaymentById(Long id) {
        log.debug("Fetching payment with ID: {}", id);

        SupplierPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public SupplierPaymentResponse getPaymentByReference(String reference) {
        log.debug("Fetching payment with reference: {}", reference);

        SupplierPayment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + reference));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public Page<SupplierPaymentResponse> getAllPayments(Pageable pageable) {
        log.debug("Fetching all payments with pagination");

        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId) {
        log.debug("Fetching payments for supplier ID: {}", supplierId);

        return paymentRepository.findBySupplierId(supplierId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId, Pageable pageable) {
        log.debug("Fetching payments for supplier ID: {} with pagination", supplierId);

        return paymentRepository.findBySupplierId(supplierId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<SupplierPaymentResponse> getPaymentsByPurchase(Long purchaseId) {
        log.debug("Fetching payments for purchase ID: {}", purchaseId);

        return paymentRepository.findByPurchaseId(purchaseId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SupplierPaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching payments between {} and {}", startDate, endDate);

        return paymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelPayment(Long id, String reason) {
        log.info("Cancelling payment with ID: {}", id);

        SupplierPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment is already cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setNotes(payment.getNotes() + " [CANCELLED: " + reason + "]");
        paymentRepository.save(payment);

        // Reverse the outstanding amount update
        supplierService.updateOutstandingAmount(payment.getSupplier().getId(), payment.getAmount());

        // Update purchase payment status if payment was for a specific purchase
        if (payment.getPurchase() != null) {
            purchaseService.updatePaymentStatus(payment.getPurchase().getId(), -payment.getAmount().doubleValue());
        }

        log.info("Payment cancelled successfully with ID: {}", id);
    }

    @Override
    public Double getTotalPaymentsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = paymentRepository.getTotalPaymentsForPeriod(startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }
}