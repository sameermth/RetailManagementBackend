package com.retailmanagement.modules.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseRequest {

    @NotBlank(message = "Warehouse code is required")
    @Size(min = 2, max = 20, message = "Warehouse code must be between 2 and 20 characters")
    private String code;

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 2, max = 100, message = "Warehouse name must be between 2 and 100 characters")
    private String name;

    private String description;

    private String address;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private String phone;

    private String email;

    private String manager;

    private Double latitude;

    private Double longitude;

    private Boolean isActive;

    private Boolean isPrimary;

    private Integer capacity;
}