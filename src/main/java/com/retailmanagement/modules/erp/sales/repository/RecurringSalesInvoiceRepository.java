package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.RecurringSalesInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringSalesInvoiceRepository extends JpaRepository<RecurringSalesInvoice, Long> {
    List<RecurringSalesInvoice> findByOrganizationIdOrderByIdDesc(Long organizationId);
    Optional<RecurringSalesInvoice> findByIdAndOrganizationId(Long id, Long organizationId);
    List<RecurringSalesInvoice> findByIsActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(LocalDate runDate);
}
