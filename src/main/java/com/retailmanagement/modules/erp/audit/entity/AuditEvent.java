package com.retailmanagement.modules.erp.audit.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        name = "audit_event",
        indexes = {
                @Index(name = "idx_erp_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_erp_audit_occurred", columnList = "occurred_at")
        }
)
public class AuditEvent extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_number")
    private String entityNumber;

    @Column(nullable = false)
    private String action;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_name_snapshot")
    private String actorNameSnapshot;

    @Column(name = "actor_role_snapshot")
    private String actorRoleSnapshot;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode payloadJson;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "ip_address")
    private String ipAddress;
}
