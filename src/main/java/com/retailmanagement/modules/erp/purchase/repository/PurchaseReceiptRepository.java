package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpPurchaseReceiptRepository")
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    List<PurchaseReceipt> findTop100ByOrganizationIdOrderByReceiptDateDescIdDesc(Long organizationId);
    List<PurchaseReceipt> findByOrganizationIdOrderByDueDateAscIdAsc(Long organizationId);
}
