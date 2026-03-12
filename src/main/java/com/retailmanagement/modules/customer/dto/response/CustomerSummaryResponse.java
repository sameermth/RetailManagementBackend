package com.retailmanagement.modules.customer.dto.response;

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
public class CustomerSummaryResponse {
    private Long id;
    private String customerCode;
    private String name;
    private String phone;
    private String email;
    private BigDecimal totalDueAmount;
    private Integer totalPurchases;
    private BigDecimal averagePurchaseValue;
    private LocalDateTime lastPurchaseDate;
    private Integer loyaltyPoints;
    private String status;
}