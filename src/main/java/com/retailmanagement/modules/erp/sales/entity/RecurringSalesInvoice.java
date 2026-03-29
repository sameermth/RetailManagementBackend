package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "recurring_sales_invoice", indexes = {
        @Index(name = "idx_recurring_sales_invoice_org_next_run", columnList = "organization_id,is_active,next_run_date")
})
public class RecurringSalesInvoice extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "price_list_id")
    private Long priceListId;

    @Column(name = "template_number", nullable = false, length = 80)
    private String templateNumber;

    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "due_days")
    private Integer dueDays;

    @Column(name = "place_of_supply_state_code", length = 8)
    private String placeOfSupplyStateCode;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_sales_invoice_id")
    private Long lastSalesInvoiceId;
}
