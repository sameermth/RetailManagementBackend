package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.RecurringJournal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringJournalRepository extends JpaRepository<RecurringJournal, Long> {
    List<RecurringJournal> findByOrganizationIdOrderByIdDesc(Long organizationId);
    Optional<RecurringJournal> findByIdAndOrganizationId(Long id, Long organizationId);
    List<RecurringJournal> findByIsActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(LocalDate runDate);
}
