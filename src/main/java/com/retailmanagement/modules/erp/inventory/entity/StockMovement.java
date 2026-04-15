package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity(name="ErpStockMovement") @Table(name="stock_movement", indexes={@Index(name="idx_erp_stock_reference", columnList="reference_type,reference_id")})
public class StockMovement extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="warehouse_id", nullable=false) private Long warehouseId;
 @Column(name="bin_location_id") private Long binLocationId;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="movement_type", nullable=false) private String movementType;
 @Column(name="reference_type", nullable=false) private String referenceType;
 @Column(name="reference_id", nullable=false) private Long referenceId;
 @Column(name="reference_number") private String referenceNumber;
 @Column(nullable=false) private String direction;
 @Column(name="uom_id", nullable=false) private Long uomId;
 @Column(nullable=false, precision=18, scale=6) private BigDecimal quantity;
 @Column(name="base_quantity", nullable=false, precision=18, scale=6) private BigDecimal baseQuantity;
 @Column(name="unit_cost", precision=18, scale=2) private BigDecimal unitCost;
 @Column(name="total_cost", precision=18, scale=2) private BigDecimal totalCost;
 @Column(name="movement_at", nullable=false) private LocalDateTime movementAt;
}
