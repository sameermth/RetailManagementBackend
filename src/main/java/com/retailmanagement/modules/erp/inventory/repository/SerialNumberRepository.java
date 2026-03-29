package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialNumberRepository extends JpaRepository<SerialNumber, Long> {
    Optional<SerialNumber> findByOrganizationIdAndSerialNumber(Long organizationId, String serialNumber);
    Optional<SerialNumber> findFirstByOrganizationIdAndSerialNumberIgnoreCase(Long organizationId, String serialNumber);
    Optional<SerialNumber> findFirstByOrganizationIdAndManufacturerSerialNumberIgnoreCase(Long organizationId, String manufacturerSerialNumber);
    List<SerialNumber> findByOrganizationIdAndProductId(Long organizationId, Long productId);
}
