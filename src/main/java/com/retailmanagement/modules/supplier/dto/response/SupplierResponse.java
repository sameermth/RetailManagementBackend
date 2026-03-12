package com.retailmanagement.modules.supplier.dto.response;

import com.retailmanagement.modules.supplier.enums.SupplierStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private String supplierCode;
    private String name;
    private SupplierStatus status;
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
    private String taxType;
    private String taxRegistrationNumber;
    private String businessType;
    private Integer leadTimeDays;
    private Integer minimumOrderValue;
    private Integer maximumOrderValue;
    private List<SupplierContactResponse> contacts;
    private Double averageRating;
    private Integer totalPurchases;
    private String notes;
    private Boolean isActive;
    private LocalDateTime lastPurchaseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}