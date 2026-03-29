package com.retailmanagement.modules.erp.common.model;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ErpAuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = ErpSecurityUtils.currentUserId().orElse(null);
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = this.createdBy == null ? currentUserId : this.createdBy;
        this.updatedBy = currentUserId == null ? this.updatedBy : currentUserId;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = ErpSecurityUtils.currentUserId().orElse(this.updatedBy);
    }
}
