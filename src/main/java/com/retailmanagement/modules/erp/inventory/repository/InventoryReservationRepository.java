package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.InventoryReservation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByOrganizationIdAndSourceDocumentTypeAndSourceDocumentIdAndStatusOrderByIdAsc(
            Long organizationId,
            String sourceDocumentType,
            Long sourceDocumentId,
            String status
    );

    List<InventoryReservation> findByOrganizationIdOrderByIdDesc(Long organizationId);

    List<InventoryReservation> findByOrganizationIdAndStatusOrderByIdDesc(Long organizationId, String status);

    List<InventoryReservation> findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAscIdAsc(String status, LocalDateTime expiresAt);
}
