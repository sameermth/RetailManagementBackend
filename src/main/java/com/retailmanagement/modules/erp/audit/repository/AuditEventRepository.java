package com.retailmanagement.modules.erp.audit.repository;

import com.retailmanagement.modules.erp.audit.entity.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop100ByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, Long entityId);
    List<AuditEvent> findTop200ByOrderByOccurredAtDescIdDesc();
}
