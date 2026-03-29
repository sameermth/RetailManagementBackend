package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="serial_number")
public class SerialNumber extends ErpOrgScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="batch_id") private Long batchId;
 @Column(name="serial_number", nullable=false) private String serialNumber;
 @Column(name="manufacturer_serial_number") private String manufacturerSerialNumber;
 @Column(nullable=false) private String status;
 @Column(name="current_warehouse_id") private Long currentWarehouseId;
 @Column(name="current_customer_id") private Long currentCustomerId;
 @Column(name="warranty_start_date") private LocalDate warrantyStartDate;
 @Column(name="warranty_end_date") private LocalDate warrantyEndDate;
}
