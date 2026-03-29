package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturnLine;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesReturnLineRepository extends JpaRepository<SalesReturnLine, Long> {
    List<SalesReturnLine> findBySalesReturnIdOrderByIdAsc(Long salesReturnId);
    List<SalesReturnLine> findByOriginalSalesInvoiceLineId(Long originalSalesInvoiceLineId);

    @Query("""
            select coalesce(sum(line.baseQuantity), 0)
            from SalesReturnLine line
            join SalesReturn header on header.id = line.salesReturnId
            where line.originalSalesInvoiceLineId = :originalSalesInvoiceLineId
              and header.status not in ('REJECTED', 'CANCELLED')
            """)
    BigDecimal sumActiveBaseQuantityByOriginalSalesInvoiceLineId(@Param("originalSalesInvoiceLineId") Long originalSalesInvoiceLineId);
}
