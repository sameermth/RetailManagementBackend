package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
 @Column(name="batch_type", nullable=false) private String batchType = "EXTERNAL_BATCH";
 @Column(name="source_document_type") private String sourceDocumentType;
 @Column(name="source_document_id") private Long sourceDocumentId;
 @Column(name="source_document_line_id") private Long sourceDocumentLineId;
 @Column(name="purchase_unit_cost", precision=18, scale=2) private BigDecimal purchaseUnitCost;
 @Column(name="suggested_sale_price", precision=18, scale=2) private BigDecimal suggestedSalePrice;
 @Column(name="mrp", precision=18, scale=2) private BigDecimal mrp;
 @Column(nullable=false) private String status;
}
