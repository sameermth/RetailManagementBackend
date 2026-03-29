package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.BankStatementEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementEntryRepository extends JpaRepository<BankStatementEntry, Long> {
    List<BankStatementEntry> findByOrganizationIdAndAccountIdOrderByEntryDateDescIdDesc(Long organizationId, Long accountId);
    List<BankStatementEntry> findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(Long organizationId, Long accountId, LocalDate fromDate, LocalDate toDate);
    Optional<BankStatementEntry> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByMatchedLedgerEntryId(Long matchedLedgerEntryId);
}
