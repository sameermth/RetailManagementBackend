package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceTicket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, Long> {
    List<ServiceTicket> findTop100ByOrganizationIdOrderByReportedOnDescIdDesc(Long organizationId);
    Optional<ServiceTicket> findByOrganizationIdAndId(Long organizationId, Long id);
}
