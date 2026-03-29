package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnLineBatchRepository extends JpaRepository<SalesReturnLineBatch, Long> {
    List<SalesReturnLineBatch> findBySalesReturnLineId(Long salesReturnLineId);
}
