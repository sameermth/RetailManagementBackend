package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stock_count_session", indexes = {
        @Index(name = "idx_stock_count_session_org", columnList = "organization_id,warehouse_id,count_date")
})
public class StockCountSession extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "count_number", nullable = false, unique = true)
    private String countNumber;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(nullable = false)
    private String status;

    @Column
    private String notes;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "variance_posted_at")
    private LocalDateTime variancePostedAt;
}
