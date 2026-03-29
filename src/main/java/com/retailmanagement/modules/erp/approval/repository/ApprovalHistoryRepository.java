package com.retailmanagement.modules.erp.approval.repository;

import com.retailmanagement.modules.erp.approval.entity.ApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByApprovalRequestIdOrderByActionAtAscIdAsc(Long approvalRequestId);
}
