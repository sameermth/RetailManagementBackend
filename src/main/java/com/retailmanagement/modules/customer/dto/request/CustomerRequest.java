package com.retailmanagement.modules.customer.dto.request;

import com.retailmanagement.modules.customer.enums.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String name;

    private CustomerType customerType;

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

    // Business specific fields
    private String businessName;
    private String contactPerson;
    private String designation;

    private BigDecimal creditLimit;

    private Integer paymentTerms;

    private Boolean dueReminderEnabled;

    private Integer reminderFrequencyDays;

    private String notes;

    private Boolean isActive;
}