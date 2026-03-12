package com.retailmanagement.modules.distributor.model;

import com.retailmanagement.modules.distributor.enums.DistributorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "distributors")
@EntityListeners(AuditingEntityListener.class)
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String distributorCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DistributorStatus status;

    private String email;

    private String phone;

    private String alternatePhone;

    @Column(length = 500)
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

    @Column(precision = 10, scale = 2)
    private BigDecimal creditLimit;

    @Column(precision = 10, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    private Integer paymentTerms; // in days

    private String paymentMethod;

    private String bankName;

    private String bankAccountNumber;

    private String bankIfscCode;

    private String bankBranch;

    private String upiId;

    private String region;

    private String territory;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate;

    private Integer deliveryTimeDays;

    private Integer minimumOrderValue;

    @OneToMany(mappedBy = "distributor", cascade = CascadeType.ALL)
    private List<DistributorOrder> orders = new ArrayList<>();

    private String notes;

    private Boolean isActive = true;

    private LocalDateTime lastOrderDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}