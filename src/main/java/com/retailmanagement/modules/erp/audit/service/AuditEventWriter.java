package com.retailmanagement.modules.erp.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.modules.erp.audit.entity.AuditEvent;
import com.retailmanagement.modules.erp.audit.repository.AuditEventRepository;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditEventWriter {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public void write(
            Long organizationId,
            Long branchId,
            String eventType,
            String entityType,
            Long entityId,
            String entityNumber,
            String action,
            Long warehouseId,
            Long customerId,
            Long supplierId,
            String summary,
            String payloadJson
    ) {
        AuditEvent event = new AuditEvent();
        event.setOrganizationId(organizationId);
        event.setBranchId(branchId);
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEntityNumber(entityNumber);
        event.setAction(action);
        Long actorUserId = ErpSecurityUtils.currentUserId().orElse(null);
        event.setActorUserId(actorUserId);
        event.setActorNameSnapshot(actorUserId == null
                ? "public-portal"
                : ErpSecurityUtils.currentUsername().orElse("system"));
        event.setActorRoleSnapshot(actorUserId == null ? "PUBLIC" : "ERP_USER");
        event.setWarehouseId(warehouseId);
        event.setCustomerId(customerId);
        event.setSupplierId(supplierId);
        event.setOccurredAt(LocalDateTime.now());
        event.setSummary(summary);
        event.setPayloadJson(parsePayload(payloadJson));
        auditEventRepository.save(event);
    }

    private JsonNode parsePayload(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid audit payload json", ex);
        }
    }
}
