package com.retailmanagement.modules.erp.imports.repository;

import com.retailmanagement.modules.erp.imports.entity.ImportJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    List<ImportJob> findTop100ByOrganizationIdOrderByStartedAtDescIdDesc(Long organizationId);
    List<ImportJob> findTop100ByOrganizationIdAndEntityTypeOrderByStartedAtDescIdDesc(Long organizationId, String entityType);
    Optional<ImportJob> findByIdAndOrganizationId(Long id, Long organizationId);
}
