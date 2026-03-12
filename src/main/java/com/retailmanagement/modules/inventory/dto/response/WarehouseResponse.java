package com.retailmanagement.modules.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String code;
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
    private Integer currentOccupancy;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}