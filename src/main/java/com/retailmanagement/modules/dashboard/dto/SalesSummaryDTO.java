package com.retailmanagement.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {
    private BigDecimal totalAmount;
    private Integer totalTransactions;
    private BigDecimal averageTransactionValue;
    private BigDecimal cashAmount;
    private BigDecimal cardAmount;
    private BigDecimal upiAmount;
    private BigDecimal creditAmount;
}