package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesReturnLineSerialRepository extends JpaRepository<SalesReturnLineSerial, Long> {
    List<SalesReturnLineSerial> findBySalesReturnLineId(Long salesReturnLineId);
    List<SalesReturnLineSerial> findBySerialNumberId(Long serialNumberId);

    @Query("""
            select count(link) > 0
            from SalesReturnLineSerial link
            join SalesReturnLine line on line.id = link.salesReturnLineId
            join SalesReturn header on header.id = line.salesReturnId
            where link.serialNumberId = :serialNumberId
              and header.status not in ('REJECTED', 'CANCELLED')
            """)
    boolean existsInActiveReturnBySerialNumberId(@Param("serialNumberId") Long serialNumberId);
}
