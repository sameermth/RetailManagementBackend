package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.LedgerEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    boolean existsByOrganizationIdAndAccountId(Long organizationId, Long accountId);
    List<LedgerEntry> findByOrganizationIdAndVoucherIdOrderByIdAsc(Long organizationId, Long voucherId);
    List<LedgerEntry> findByOrganizationIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    List<LedgerEntry> findByOrganizationIdAndCustomerIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(Long organizationId, Long customerId, LocalDate fromDate, LocalDate toDate);
    List<LedgerEntry> findByOrganizationIdAndSupplierIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(Long organizationId, Long supplierId, LocalDate fromDate, LocalDate toDate);
    List<LedgerEntry> findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(Long organizationId, Long accountId, LocalDate fromDate, LocalDate toDate);
}
