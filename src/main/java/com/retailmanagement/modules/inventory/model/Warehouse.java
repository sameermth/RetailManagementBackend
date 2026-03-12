package com.retailmanagement.modules.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses")
@EntityListeners(AuditingEntityListener.class)
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
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

    @Column(precision = 10, scale = 6)
    private Double latitude;

    @Column(precision = 10, scale = 6)
    private Double longitude;

    private Boolean isActive = true;

    private Boolean isPrimary = false;

    private Integer capacity;

    private Integer currentOccupancy;

    @OneToMany(mappedBy = "warehouse")
    private List<Inventory> inventories = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}