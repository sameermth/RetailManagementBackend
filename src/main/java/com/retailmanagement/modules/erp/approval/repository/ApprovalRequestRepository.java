package com.retailmanagement.modules.erp.approval.repository;

import com.retailmanagement.modules.erp.approval.entity.ApprovalRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findTop100ByOrganizationIdOrderByRequestedAtDescIdDesc(Long organizationId);
    List<ApprovalRequest> findTop100ByOrganizationIdAndStatusOrderByRequestedAtDescIdDesc(Long organizationId, String status);
    List<ApprovalRequest> findByOrganizationIdAndStatusOrderByRequestedAtDescIdDesc(Long organizationId, String status);
    Optional<ApprovalRequest> findByOrganizationIdAndEntityTypeAndEntityIdAndStatus(Long organizationId, String entityType, Long entityId, String status);
}
