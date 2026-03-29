package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesLineBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesLineBatchRepository extends JpaRepository<SalesLineBatch, Long> {
    List<SalesLineBatch> findBySalesInvoiceLineId(Long salesInvoiceLineId);
}
