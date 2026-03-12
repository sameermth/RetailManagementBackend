package com.retailmanagement.modules.distributor.dto.response;

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
public class DistributorOrderSummaryResponse {
    private Long id;
    private String orderNumber;
    private String distributorName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String paymentStatus;
}