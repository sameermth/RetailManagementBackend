package com.retailmanagement.modules.purchase.dto.response;

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
public class PurchaseSummaryResponse {
    private Long id;
    private String purchaseOrderNumber;
    private String supplierName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String paymentStatus;
}