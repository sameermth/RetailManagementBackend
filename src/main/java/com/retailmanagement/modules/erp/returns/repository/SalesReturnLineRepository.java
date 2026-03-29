package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturnLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnLineRepository extends JpaRepository<SalesReturnLine, Long> {
    List<SalesReturnLine> findBySalesReturnIdOrderByIdAsc(Long salesReturnId);
    List<SalesReturnLine> findByOriginalSalesInvoiceLineId(Long originalSalesInvoiceLineId);
}
