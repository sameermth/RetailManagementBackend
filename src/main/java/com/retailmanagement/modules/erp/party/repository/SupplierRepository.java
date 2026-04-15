package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpSupplierRepository")
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByOrganizationId(Long organizationId);
    Optional<Supplier> findByOrganizationIdAndSupplierCodeIgnoreCase(Long organizationId, String supplierCode);
    boolean existsByOrganizationIdAndSupplierCode(Long organizationId, String supplierCode);
    boolean existsByOrganizationIdAndSupplierCodeAndIdNot(Long organizationId, String supplierCode, Long id);
    Optional<Supplier> findByOrganizationIdAndId(Long organizationId, Long id);
    Optional<Supplier> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByEmail(String email);
}
