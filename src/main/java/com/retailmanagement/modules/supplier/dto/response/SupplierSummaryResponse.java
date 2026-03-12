package com.retailmanagement.modules.supplier.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSummaryResponse {
    private Long id;
    private String supplierCode;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String city;
    private String status;
    private BigDecimal outstandingAmount;
    private Integer totalPurchases;
    private Double averageRating;
    private Integer leadTimeDays;
}