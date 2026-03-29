package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceLineRepository extends JpaRepository<SalesInvoiceLine, Long> {
    List<SalesInvoiceLine> findBySalesInvoiceIdOrderByIdAsc(Long salesInvoiceId);
}
