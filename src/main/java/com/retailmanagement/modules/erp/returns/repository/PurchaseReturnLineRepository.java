package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLine;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReturnLineRepository extends JpaRepository<PurchaseReturnLine, Long> {
    List<PurchaseReturnLine> findByPurchaseReturnIdOrderByIdAsc(Long purchaseReturnId);
    List<PurchaseReturnLine> findByOriginalPurchaseReceiptLineId(Long originalPurchaseReceiptLineId);

    @Query("""
            select coalesce(sum(line.baseQuantity), 0)
            from PurchaseReturnLine line
            join PurchaseReturn header on header.id = line.purchaseReturnId
            where line.originalPurchaseReceiptLineId = :originalPurchaseReceiptLineId
              and header.status not in ('REJECTED', 'CANCELLED')
            """)
    BigDecimal sumActiveBaseQuantityByOriginalPurchaseReceiptLineId(@Param("originalPurchaseReceiptLineId") Long originalPurchaseReceiptLineId);
}
