package com.retailmanagement.modules.customer.dto.response;

import com.retailmanagement.modules.customer.enums.CustomerType;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
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
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String name;
    private CustomerType customerType;
    private CustomerStatus status;
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
    private String businessName;
    private String contactPerson;
    private String designation;
    private BigDecimal creditLimit;
    private BigDecimal totalDueAmount;
    private Integer paymentTerms;
    private Boolean dueReminderEnabled;
    private Integer reminderFrequencyDays;
    private LocalDateTime lastDueDate;
    private Integer loyaltyPoints;
    private String loyaltyTier;
    private BigDecimal totalPurchaseAmount;
    private LocalDateTime lastPurchaseDate;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}