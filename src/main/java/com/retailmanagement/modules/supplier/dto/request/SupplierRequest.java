package com.retailmanagement.modules.supplier.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
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

    private String taxType;

    private String taxRegistrationNumber;

    private String businessType;

    private Integer leadTimeDays;

    private Integer minimumOrderValue;

    private Integer maximumOrderValue;

    private List<SupplierContactRequest> contacts;

    private String notes;

    private Boolean isActive;
}