package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnLineSerialRepository extends JpaRepository<SalesReturnLineSerial, Long> {
    List<SalesReturnLineSerial> findBySalesReturnLineId(Long salesReturnLineId);
    List<SalesReturnLineSerial> findBySerialNumberId(Long serialNumberId);
}
