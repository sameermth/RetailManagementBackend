package com.retailmanagement.modules.distributor.dto.response;

import com.retailmanagement.modules.distributor.enums.DistributorStatus;
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
public class DistributorResponse {
    private Long id;
    private String distributorCode;
    private String name;
    private DistributorStatus status;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String gstNumber;
    private String panNumber;
    private String website;
    private String contactPerson;
    private String contactPersonPhone;
    private String contactPersonEmail;
    private BigDecimal creditLimit;
    private BigDecimal outstandingAmount;
    private Integer paymentTerms;
    private String paymentMethod;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankBranch;
    private String upiId;
    private String region;
    private String territory;
    private BigDecimal commissionRate;
    private Integer deliveryTimeDays;
    private Integer minimumOrderValue;
    private Integer totalOrders;
    private BigDecimal totalOrderValue;
    private String notes;
    private Boolean isActive;
    private LocalDateTime lastOrderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}