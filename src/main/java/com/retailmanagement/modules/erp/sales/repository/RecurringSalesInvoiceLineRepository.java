package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.RecurringSalesInvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringSalesInvoiceLineRepository extends JpaRepository<RecurringSalesInvoiceLine, Long> {
    List<RecurringSalesInvoiceLine> findByRecurringSalesInvoiceIdOrderByIdAsc(Long recurringSalesInvoiceId);
}
