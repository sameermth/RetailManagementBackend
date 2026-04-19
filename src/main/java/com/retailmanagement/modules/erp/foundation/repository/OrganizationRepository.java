package com.retailmanagement.modules.erp.foundation.repository;

import com.retailmanagement.modules.erp.foundation.entity.Organization;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);
    Optional<Organization> findTopByOrderByIdDesc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    List<Organization> findByIdIn(Collection<Long> ids);
    long countByOwnerAccountIdAndIsActiveTrue(Long ownerAccountId);
    List<Organization> findByOwnerAccountId(Long ownerAccountId);
}
