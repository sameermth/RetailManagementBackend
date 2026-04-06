package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.RecurringJournalLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringJournalLineRepository extends JpaRepository<RecurringJournalLine, Long> {
    boolean existsByAccountId(Long accountId);
    List<RecurringJournalLine> findByRecurringJournalIdOrderByIdAsc(Long recurringJournalId);
}
