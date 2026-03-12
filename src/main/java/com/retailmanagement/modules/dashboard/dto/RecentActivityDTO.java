package com.retailmanagement.modules.dashboard.dto;

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
public class RecentActivityDTO {
    private Long id;
    private String type; // SALE, PURCHASE, PAYMENT, CUSTOMER, etc.
    private String description;
    private String reference;
    private String user;
    private LocalDateTime timestamp;
    private String status;
    private BigDecimal amount;
}