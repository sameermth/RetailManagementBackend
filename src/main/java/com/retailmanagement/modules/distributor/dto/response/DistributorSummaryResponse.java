package com.retailmanagement.modules.distributor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributorSummaryResponse {
    private Long id;
    private String distributorCode;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String city;
    private String region;
    private String status;
    private BigDecimal outstandingAmount;
    private Integer totalOrders;
    private BigDecimal commissionRate;
    private Integer deliveryTimeDays;
}