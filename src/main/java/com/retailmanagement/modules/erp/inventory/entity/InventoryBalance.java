package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="inventory_balance")
public class InventoryBalance extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="warehouse_id", nullable=false) private Long warehouseId;
 @Column(name="bin_location_id") private Long binLocationId;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="batch_id") private Long batchId;
 @Column(name="on_hand_base_quantity", nullable=false, precision=18, scale=6) private BigDecimal onHandBaseQuantity;
 @Column(name="reserved_base_quantity", nullable=false, precision=18, scale=6) private BigDecimal reservedBaseQuantity;
 @Column(name="available_base_quantity", nullable=false, precision=18, scale=6) private BigDecimal availableBaseQuantity;
 @Column(name="avg_cost", precision=18, scale=2) private BigDecimal avgCost;
}
