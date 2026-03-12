package com.retailmanagement.modules.purchase.dto.response;

import com.retailmanagement.modules.purchase.enums.PaymentMethod;
import com.retailmanagement.modules.purchase.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPaymentResponse {
    private Long id;
    private String paymentReference;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private Long purchaseId;
    private String purchaseOrderNumber;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String bankName;
    private String chequeNumber;
    private LocalDateTime chequeDate;
    private String cardLastFour;
    private String cardType;
    private String upiId;
    private String notes;
    private String receivedBy;
    private LocalDateTime createdAt;
}