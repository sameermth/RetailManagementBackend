package com.retailmanagement.modules.distributor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DistributorRequest {

    @NotBlank(message = "Distributor name is required")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
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

    private String notes;

    private Boolean isActive;
}