package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesDispatchLine;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesDispatchLineRepository extends JpaRepository<SalesDispatchLine, Long> {

    List<SalesDispatchLine> findBySalesDispatchIdOrderByIdAsc(Long salesDispatchId);

    List<SalesDispatchLine> findBySalesDispatchIdIn(Collection<Long> salesDispatchIds);
}
