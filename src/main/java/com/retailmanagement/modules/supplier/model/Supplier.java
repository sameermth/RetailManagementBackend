package com.retailmanagement.modules.supplier.model;

import com.retailmanagement.modules.purchase.model.Purchase;
import com.retailmanagement.modules.supplier.enums.SupplierStatus;
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
@Table(name = "suppliers")
@EntityListeners(AuditingEntityListener.class)
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String supplierCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private SupplierStatus status;

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

    private String taxType; // GST, VAT, etc.

    private String taxRegistrationNumber;

    private String businessType; // Manufacturer, Distributor, Wholesaler, etc.

    private Integer leadTimeDays; // Average delivery time

    private Integer minimumOrderValue;

    private Integer maximumOrderValue;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "supplier")
    private List<Purchase> purchases = new ArrayList<>();

    private String notes;

    private Boolean isActive = true;

    private LocalDateTime lastPurchaseDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}