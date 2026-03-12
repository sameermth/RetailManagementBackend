package com.retailmanagement.modules.supplier.dto.request;

import lombok.Data;

@Data
public class SupplierContactRequest {

    private String name;

    private String designation;

    private String department;

    private String email;

    private String phone;

    private String mobile;

    private Boolean isPrimary;

    private String notes;
}