package com.retailmanagement.modules.supplier.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier_ratings")
public class SupplierRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private Integer qualityRating; // 1-5

    private Integer deliveryRating; // 1-5

    private Integer priceRating; // 1-5

    private Integer communicationRating; // 1-5

    private Double averageRating;

    private String comments;

    private Long purchaseId;

    private String ratedBy;

    private LocalDateTime ratedAt;
}