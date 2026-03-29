package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.StoreSupplierTerms;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpStoreSupplierTermsRepository")
public interface StoreSupplierTermsRepository extends JpaRepository<StoreSupplierTerms, Long> {
    Optional<StoreSupplierTerms> findByOrganizationIdAndSupplierId(Long organizationId, Long supplierId);
}
