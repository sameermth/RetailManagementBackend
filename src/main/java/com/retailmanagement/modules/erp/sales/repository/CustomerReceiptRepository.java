package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, Long> {
    List<CustomerReceipt> findTop100ByOrganizationIdOrderByReceiptDateDescIdDesc(Long organizationId);
    List<CustomerReceipt> findByOrganizationIdOrderByReceiptDateDescIdDesc(Long organizationId);
    List<CustomerReceipt> findByOrganizationIdAndPosSessionIdOrderByReceiptDateDescIdDesc(Long organizationId, Long posSessionId);
}
