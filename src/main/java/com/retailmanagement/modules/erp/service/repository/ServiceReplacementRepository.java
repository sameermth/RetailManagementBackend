package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceReplacementRepository extends JpaRepository<ServiceReplacement, Long> {
    List<ServiceReplacement> findTop100ByOrganizationIdOrderByIssuedOnDescIdDesc(Long organizationId);
    List<ServiceReplacement> findByOrganizationIdAndIssuedOnBetweenOrderByIssuedOnDescIdDesc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    Optional<ServiceReplacement> findByOrganizationIdAndId(Long organizationId, Long id);
    boolean existsByWarrantyClaimIdAndStatus(Long warrantyClaimId, String status);
    boolean existsByServiceTicketIdAndStatus(Long serviceTicketId, String status);
    boolean existsByOriginalProductOwnershipIdAndStatus(Long originalProductOwnershipId, String status);
    boolean existsBySalesReturnIdAndStatus(Long salesReturnId, String status);
}
