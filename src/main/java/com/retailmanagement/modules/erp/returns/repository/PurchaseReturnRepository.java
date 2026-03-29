package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {
    List<PurchaseReturn> findTop100ByOrganizationIdOrderByReturnDateDescIdDesc(Long organizationId);
    List<PurchaseReturn> findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    List<PurchaseReturn> findByOrganizationIdAndOriginalPurchaseReceiptIdAndStatusAndReturnDateLessThanEqual(Long organizationId, Long originalPurchaseReceiptId, String status, LocalDate asOfDate);
}
