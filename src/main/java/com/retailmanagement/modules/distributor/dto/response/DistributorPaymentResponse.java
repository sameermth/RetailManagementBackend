package com.retailmanagement.modules.distributor.dto.response;

import com.retailmanagement.modules.distributor.enums.PaymentMethod;
import com.retailmanagement.modules.distributor.enums.PaymentStatus;
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
public class DistributorPaymentResponse {
    private Long id;
    private String paymentReference;
    private Long distributorId;
    private String distributorName;
    private String distributorCode;
    private Long orderId;
    private String orderNumber;
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