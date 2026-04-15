package com.retailmanagement.modules.erp.imports.repository;

import com.retailmanagement.modules.erp.imports.entity.ImportJobRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRowRepository extends JpaRepository<ImportJobRow, Long> {
    List<ImportJobRow> findByImportJobIdOrderByRowNumberAscIdAsc(Long importJobId);
    List<ImportJobRow> findByImportJobIdAndStatusOrderByRowNumberAscIdAsc(Long importJobId, String status);
}
