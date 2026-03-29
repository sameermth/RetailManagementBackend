package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="inventory_batch")
public class InventoryBatch extends ErpOrgScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="batch_number", nullable=false) private String batchNumber;
 @Column(name="manufacturer_batch_number") private String manufacturerBatchNumber;
 @Column(name="manufactured_on") private LocalDate manufacturedOn;
 @Column(name="expiry_on") private LocalDate expiryOn;
 @Column(nullable=false) private String status;
}
