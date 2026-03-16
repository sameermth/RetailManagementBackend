package com.retailmanagement.modules.inventory.model;

import com.retailmanagement.modules.product.model.Product;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "warehouse_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity = 0;

    private Integer reservedQuantity = 0;

    private Integer availableQuantity = 0;

    private Integer minimumStock;

    private Integer maximumStock;

    private Integer reorderPoint;

    private Integer reorderQuantity;

    private String binLocation;

    private String shelfNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal averageCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal lastPurchasePrice;

    private LocalDateTime lastStockTakeDate;

    private LocalDateTime lastMovementDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateAvailableQuantity() {
        this.availableQuantity = this.quantity - this.reservedQuantity;
    }
}