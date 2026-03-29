package com.retailmanagement.modules.erp.audit.controller;

import com.retailmanagement.modules.erp.audit.entity.AuditEvent;
import com.retailmanagement.modules.erp.audit.repository.AuditEventRepository;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/erp/audit-events") @RequiredArgsConstructor
@Tag(name = "ERP Audit", description = "ERP audit event query endpoints")
public class AuditEventController {
 private final AuditEventRepository repository;
 @GetMapping @Operation(summary = "List audit events by entity") @PreAuthorize("hasAuthority('approval.manage')") public ErpApiResponse<List<AuditEventResponse>> byEntity(@RequestParam String entityType, @RequestParam Long entityId){
   return ErpApiResponse.ok(repository.findTop100ByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId).stream().map(this::toResponse).toList());
 }

 private AuditEventResponse toResponse(AuditEvent event) {
  return new AuditEventResponse(event.getId(), event.getOrganizationId(), event.getBranchId(), event.getEventType(), event.getEntityType(),
          event.getEntityId(), event.getEntityNumber(), event.getAction(), event.getActorUserId(), event.getActorNameSnapshot(),
          event.getActorRoleSnapshot(), event.getWarehouseId(), event.getCustomerId(), event.getSupplierId(), event.getOccurredAt(),
          event.getSummary(), event.getPayloadJson(), event.getDeviceId(), event.getAppVersion(), event.getIpAddress(),
          event.getCreatedAt(), event.getUpdatedAt());
 }

 public record AuditEventResponse(
         Long id,
         Long organizationId,
         Long branchId,
         String eventType,
         String entityType,
         Long entityId,
         String entityNumber,
         String action,
         Long actorUserId,
         String actorNameSnapshot,
         String actorRoleSnapshot,
         Long warehouseId,
         Long customerId,
         Long supplierId,
         LocalDateTime occurredAt,
         String summary,
         Object payloadJson,
         String deviceId,
         String appVersion,
         String ipAddress,
         LocalDateTime createdAt,
         LocalDateTime updatedAt
 ) {}
}
