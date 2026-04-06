package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAgreementRepository extends JpaRepository<ServiceAgreement, Long> {
    List<ServiceAgreement> findTop100ByOrganizationIdOrderByServiceStartDateDescIdDesc(Long organizationId);
    Optional<ServiceAgreement> findByOrganizationIdAndId(Long organizationId, Long id);
}
