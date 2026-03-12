package com.retailmanagement.modules.supplier.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierContactResponse {
    private Long id;
    private String name;
    private String designation;
    private String department;
    private String email;
    private String phone;
    private String mobile;
    private Boolean isPrimary;
    private String notes;
}