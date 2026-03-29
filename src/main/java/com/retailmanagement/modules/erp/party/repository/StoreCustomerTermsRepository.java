package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.StoreCustomerTerms;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCustomerTermsRepository extends JpaRepository<StoreCustomerTerms, Long> {
    Optional<StoreCustomerTerms> findByOrganizationIdAndCustomerId(Long organizationId, Long customerId);
}
