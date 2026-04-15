package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.BankStatementImportBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementImportBatchRepository extends JpaRepository<BankStatementImportBatch, Long> {
    List<BankStatementImportBatch> findTop100ByOrganizationIdOrderByImportedAtDescIdDesc(Long organizationId);
    List<BankStatementImportBatch> findTop100ByOrganizationIdAndAccountIdOrderByImportedAtDescIdDesc(Long organizationId, Long accountId);
    Optional<BankStatementImportBatch> findByIdAndOrganizationId(Long id, Long organizationId);
}
