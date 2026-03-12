package com.retailmanagement.modules.sales.dto.request;

import com.retailmanagement.modules.sales.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRequest {

    @NotNull(message = "Sale ID is required")
    private Long saleId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private LocalDateTime paymentDate;

    private String transactionId;

    private String bankName;

    private String chequeNumber;

    private LocalDateTime chequeDate;

    private String cardLastFour;

    private String cardType;

    private String upiId;

    private String notes;
}