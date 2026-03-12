package com.retailmanagement.modules.sales.dto.response;

import com.retailmanagement.modules.sales.enums.PaymentMethod;
import com.retailmanagement.modules.sales.enums.PaymentStatus;
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
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private Long saleId;
    private String invoiceNumber;
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