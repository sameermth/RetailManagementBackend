package com.retailmanagement.modules.erp.approval.repository;

import com.retailmanagement.modules.erp.approval.entity.ApprovalRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {
    List<ApprovalRule> findByOrganizationIdAndActiveTrueOrderByPriorityOrderAsc(Long organizationId);
    List<ApprovalRule> findByOrganizationIdAndEntityTypeAndActiveTrueOrderByPriorityOrderAsc(Long organizationId, String entityType);
}
