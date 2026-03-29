package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesLineSerialRepository extends JpaRepository<SalesLineSerial, Long> {
    List<SalesLineSerial> findBySalesInvoiceLineId(Long salesInvoiceLineId);
}
