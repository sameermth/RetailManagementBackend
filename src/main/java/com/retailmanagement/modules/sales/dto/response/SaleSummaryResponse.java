package com.retailmanagement.modules.sales.dto.response;

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
public class SaleSummaryResponse {
    private Long id;
    private String invoiceNumber;
    private String customerName;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private String status;
    private Integer itemCount;
}