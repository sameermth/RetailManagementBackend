package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReturnLineSerialRepository extends JpaRepository<PurchaseReturnLineSerial, Long> {
    List<PurchaseReturnLineSerial> findByPurchaseReturnLineId(Long purchaseReturnLineId);
    List<PurchaseReturnLineSerial> findBySerialNumberId(Long serialNumberId);

    @Query("""
            select count(link) > 0
            from PurchaseReturnLineSerial link
            join PurchaseReturnLine line on line.id = link.purchaseReturnLineId
            join PurchaseReturn header on header.id = line.purchaseReturnId
            where link.serialNumberId = :serialNumberId
              and header.status not in ('REJECTED', 'CANCELLED')
            """)
    boolean existsInActiveReturnBySerialNumberId(@Param("serialNumberId") Long serialNumberId);
}
