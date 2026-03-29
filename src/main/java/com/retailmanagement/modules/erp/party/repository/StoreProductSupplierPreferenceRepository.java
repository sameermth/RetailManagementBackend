package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.StoreProductSupplierPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreProductSupplierPreferenceRepository extends JpaRepository<StoreProductSupplierPreference, Long> {
    Optional<StoreProductSupplierPreference> findByOrganizationIdAndStoreProductId(Long organizationId, Long storeProductId);
    Optional<StoreProductSupplierPreference> findByOrganizationIdAndStoreProductIdAndIsActiveTrue(Long organizationId, Long storeProductId);
}
