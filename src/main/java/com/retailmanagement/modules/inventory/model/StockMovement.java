package com.retailmanagement.modules.inventory.model;

import com.retailmanagement.modules.inventory.enums.MovementType;
import com.retailmanagement.modules.product.model.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    private Integer previousStock;

    private Integer newStock;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;

    private String referenceType; // PURCHASE_ORDER, SALES_ORDER, TRANSFER, ADJUSTMENT, RETURN

    private Long referenceId;

    private String reason;

    private String notes;

    private String performedBy;

    @CreatedDate
    private LocalDateTime movementDate;

    private LocalDateTime createdAt;
}