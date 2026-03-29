package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceVisit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long> {
    List<ServiceVisit> findByServiceTicketIdOrderByScheduledAtAscIdAsc(Long serviceTicketId);
    List<ServiceVisit> findTop100ByOrganizationIdOrderByScheduledAtDescIdDesc(Long organizationId);
}
